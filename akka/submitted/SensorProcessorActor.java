package com.lab.evaluation23;

import akka.actor.AbstractActor;
import akka.actor.Props;

public class SensorProcessorActor extends AbstractActor {

    private double currentAverage = 0.0;
    private int counter = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(TemperatureMsg.class, this::gotData).build();
    }

    private void gotData(TemperatureMsg msg) throws Exception {
        System.out.println("SENSOR PROCESSOR " + self() + ": Got data from " + msg.getSender());

        if (msg.getTemperature() >= 0) {
            currentAverage = ((currentAverage * counter) + msg.getTemperature()) / (++counter);
        } else {
            throw new Exception("Negative temperature reading!");
        }

        System.out.format("SENSOR PROCESSOR " + self() + ": Current avg is %.2f%s", currentAverage, System.lineSeparator());
    }

    static Props props() {
        return Props.create(SensorProcessorActor.class);
    }

    public SensorProcessorActor() {
    }
}
