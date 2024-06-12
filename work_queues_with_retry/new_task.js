#!/usr/bin/env node

var amqp = require('amqplib/callback_api');

amqp.connect('amqp://localhost', function(error0, connection) {
  if (error0) {
    throw error0;
  }
  connection.createChannel(function(error1, channel) {
    if (error1) {
      throw error1;
    }
    const queue = 'task_queue';
    const msg = process.argv.slice(2).join(' ') || "Hello World!";

    const dlxExchange = 'dlx_exchange';
    const dlxQueue = 'dlx_queue';
    const dlxKey = 'dlx_key';

    // Assert DLX Exchange and Queue
    channel.assertExchange(dlxExchange, 'direct', { durable: true });
    channel.assertQueue(dlxQueue, { durable: true });
    channel.bindQueue(dlxQueue, dlxExchange, dlxKey);

    // Assert Main Queue with DLX and TTL settings
    channel.assertQueue(queue, {
      durable: true,
      arguments: {
        "x-dead-letter-exchange": dlxExchange,
        "x-dead-letter-routing-key": dlxKey,
        'x-message-ttl': 20000
      }
    });

    channel.publish(dlxExchange, '', Buffer.from("eiei"));

    // Send message to Main Queue
    channel.sendToQueue(queue, Buffer.from(msg), {
      persistent: true
    });
    console.log(" [x] Sent '%s'", msg);
  });

  // Close connection after a short delay
  setTimeout(function() {
    connection.close();
    process.exit(0);
  }, 500); // Wait longer than the TTL to allow for message to be moved to DLX
});
