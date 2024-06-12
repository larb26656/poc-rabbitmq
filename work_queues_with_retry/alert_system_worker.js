#!/usr/bin/env node

var amqp = require('amqplib/callback_api');

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

amqp.connect('amqp://localhost', function(error0, connection) {
  if (error0) {
    throw error0;
  }
  connection.createChannel(function(error1, channel) {
    if (error1) {
      throw error1;
    }
    const dlxQueue = 'dlx_queue';

    channel.prefetch(1);
    channel.consume(dlxQueue, async msg => {
      console.log(`Received message from DLX: ${msg.content.toString()}`);
      console.log(msg);
      await delay(2000);
      console.log(`Notify to alert system: ${msg.content.toString()}`);
      channel.ack(msg);
    }, {
      noAck: false
    });
  });
});
