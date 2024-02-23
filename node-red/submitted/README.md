# Evaluation lab - Node-RED

## Group number: 14

## Group members

- Andrea Caravano
- Biagio Cancelliere
- Marco Pozzi

## Description of message flows

At the start of the flow, we initialize the /data/counter file (saved in the default Docker container folder)
to count forecast and wind requests, independently from the city (or day).
The counter in the file is then updated every minute through a flow shared object, that resets after each file writing.

Received messages are recognized through a switch block, containg all possible queries from the user (and exceptional
cases).
The requested city is parsed and passed to the corresponding OpenWeather block, after saving the original message
content in another variable, to identify the user query through a managerial function.

In the managerial function block we identify the user query and answer it through OpenWeather data received.
In particular, we look for msg.payload[8] and [16] for both wind.speed and weather[0].description.
The response message contains the forecast divided in 3 hours blocks, so the 8th and the 16th are in 24 hours and 48
hours from the current block, respectively.

Finally, we update the context variables with the last request served (and its type), together with the flow counter.

In case an unexpected query is received, a proper alert message is sent to the user.

## Extensions

The same as regular labs:
https://flows.nodered.org/node/node-red-contrib-chatbot

https://flows.nodered.org/node/node-red-node-openweathermap

## Bot URL

https://t.me/nsds_14_2023_bot

API Key: <omitted>

OpenWeather API Key: <omitted>