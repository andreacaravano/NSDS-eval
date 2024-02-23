# Evaluation lab - Apache Kafka

## Group number: 14

## Group members

- Andrea Caravano
- Biagio Cancelliere
- Marco Pozzi

Setup: Docker (Compose), updated to the latest versions of Kafka and Zookeper + Java 21

## Exercise 1

- Number of partitions allowed for inputTopic (1, 1000) (see following note)
- Number of consumers allowed (1, N) (see following note)
    - Consumer 1: groupID
    - Consumer 2: groupID
    - ...
    - Consumer n: groupID

In the case the consumer is expected to print all messages
produced from the producer that are above the threshold
himself, only one consumer per consumer group is allowed.

Otherwise, in the case there should be at most one consumer
printing a message produced by the producer that is above
the threshold, then N consumers are allowed in a consumer group.

Moreover, note that at most 1000 partitions are meaningful,
because at most 1000 keys are used (and Kafka guarantees
the same key is put in the same partition all times).

## Exercise 2

- Number of partitions allowed for inputTopic (1, 1000) (see note)
- Number of consumers allowed (1, 1) (see note below)
    - Consumer 1: groupID1
    - Consumer 2: groupID2
    - ...
    - Consumer n: groupIDn

Since the key count is kept by a data structure (Map) in the
consumer (and persistency is not guaranteed), there should
be at most one consumer
(therefore, one consumer per consumer group).

As in the previous, 1000 partitions are meaningful.