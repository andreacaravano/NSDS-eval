# Evaluation lab - Akka

## Group number: 14

## Group members

- Andrea Caravano
- Biagio Cancelliere
- Marco Pozzi

## Description of message flows
Initially, the reference of the dispatcher is made known to all temperature sensors. It is sent with a ConfigurationMessage.
Also, all the reference of the processors are created in the dispatcher actor, that manages faults with the resume strategy (to retain the average computed). 
A temperature sensor sends a new temperature with a TemperatureMsg to the dispatcher.
The dispatcher forwards the received temperature to the processors with a TemperatureMsg, based on the current policy, managed through Akka's context Receive strategy.
The dispatcher can change its policy with a DispatchLogicMsg, which contains an attribute that can be ROUND_ROBIN or LOAD_BALANCER.