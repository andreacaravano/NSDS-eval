package com.lab.evaluation23;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DispatcherActor extends AbstractActorWithStash {
    private final static int NO_PROCESSORS = 3;
    private final static ActorRef[] processors = new ActorRef[NO_PROCESSORS];

    // TemperatureSensorActor -> SensorProcessorActor
    private final static Map<ActorRef, ActorRef> tempToProc = new HashMap<>();
    private final static Map<ActorRef, Integer> procToTempCount = new HashMap<>();

    // We start counting from -1 because we use the module to discriminate between processors
    // (-1+1) % NO_PROCESSORS = 0
    // and, due to this logic, the index will never be -1 again
    private static int lastProcessorSentRR = -1;


    private static SupervisorStrategy strategy =
            new OneForOneStrategy(
                    1,
                    Duration.ofMinutes(1),
                    DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.resume()).build() // retain data structures
            );

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public DispatcherActor() {
        for (int i = 0; i < NO_PROCESSORS; i++) {
            processors[i] = getContext().actorOf(SensorProcessorActor.props());
            procToTempCount.put(processors[i], 0); // We assume that when a processor is created it is added to the counter map
        }
    }

    public Receive loadBalancing() {
        return receiveBuilder()
                .match(DispatchLogicMsg.class, this::onLogicChange)
                .match(TemperatureMsg.class, this::dispatchDataLoadBalancer)
                .build();
    }

    public Receive roundRobin() {
        return receiveBuilder()
                .match(DispatchLogicMsg.class, this::onLogicChange)
                .match(TemperatureMsg.class, this::dispatchDataRoundRobin)
                .build();
    }

    private void onLogicChange(DispatchLogicMsg msg) {
        if (msg.getLogic() == DispatchLogicMsg.LOAD_BALANCER) {
            System.out.println("Changing mode to LOAD_BALANCING...");
            getContext().become(loadBalancing());
        } else {
            System.out.println("Changing mode to ROUND_ROBIN...");
            getContext().become(roundRobin());
        }
    }


    @Override
    public AbstractActor.Receive createReceive() {
        return loadBalancing(); // default
    }

    private void dispatchDataLoadBalancer(TemperatureMsg msg) {
        int max = Integer.MIN_VALUE;
        Map.Entry<ActorRef, Integer> maxEntry = null;
        int min = Integer.MAX_VALUE;
        Map.Entry<ActorRef, Integer> minEntry = null;
        int lastProcessorSentLB = -1; // see note above (lastProcessorSentRR)

        for (Map.Entry<ActorRef, Integer> e : procToTempCount.entrySet()) {
            if (e.getValue() < min) {
                min = e.getValue();
                minEntry = e;
            } else {
                max = e.getValue();
                maxEntry = e;
            }
        }

        // In case sensors are unbalanced, balance all temperature sensors
        if (max - min >= 2) {
            List<ActorRef> tempSensors = new ArrayList<>();

            for (Map.Entry<ActorRef, ActorRef> e : tempToProc.entrySet()) {
                tempSensors.add(e.getKey());
            }

            tempToProc.clear();
            procToTempCount.clear();

            for (ActorRef ref : tempSensors) {
                // Ordered logic of matching (similar to Round Robin)
                tempToProc.put(ref, processors[(lastProcessorSentLB + 1) % NO_PROCESSORS]);
                procToTempCount.put(processors[(lastProcessorSentLB + 1) % NO_PROCESSORS],
                        procToTempCount.containsKey(processors[(lastProcessorSentLB + 1) % NO_PROCESSORS]) ?
                                procToTempCount.get(processors[(lastProcessorSentLB + 1) % NO_PROCESSORS]) + 1 : 1);
                lastProcessorSentLB = (lastProcessorSentLB + 1) % NO_PROCESSORS;
            }
        }

        //in case the sensor is not in the map
        if (!tempToProc.containsKey(msg.getSender())) {
            min = Integer.MAX_VALUE;
            minEntry = null;

            for (Map.Entry<ActorRef, Integer> e : procToTempCount.entrySet()) {
                if (e.getValue() < min) {
                    min = e.getValue();
                    minEntry = e;
                }
            }

            tempToProc.put(msg.getSender(), minEntry.getKey());

            procToTempCount.replace(minEntry.getKey(), minEntry.getValue() + 1);
        }

        System.out.format("Sending temperature message from %s sent to processor %s\n", msg.getSender(), tempToProc.get(msg.getSender()));

        tempToProc.get(msg.getSender()).tell(msg, self());
    }

    private void dispatchDataRoundRobin(TemperatureMsg msg) {
        processors[(lastProcessorSentRR + 1) % NO_PROCESSORS].tell(msg, self());

        System.out.format("Sending temperature message from %s sent to processor %s\n", sender(), processors[(lastProcessorSentRR + 1) % NO_PROCESSORS]);

        lastProcessorSentRR = (lastProcessorSentRR + 1) % NO_PROCESSORS;
    }

    static Props props() {
        return Props.create(DispatcherActor.class);
    }
}
