package com.lab.evaluation23;

import akka.actor.ActorRef;

public class ConfigurationMessage {
    private final ActorRef dispatcher;

    public ConfigurationMessage(ActorRef dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ActorRef getDispatcher() {
        return dispatcher;
    }
}
