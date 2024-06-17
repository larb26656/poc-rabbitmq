#!/usr/bin/env node

var amqp = require("amqplib/callback_api");

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function messageHandler(content, roundToExecute) {
  const [topic, maxFail, durationInSecs] = content.split("_");
  const durationInSecsNum = durationInSecs ? Number(durationInSecs) : 1;
  const maxFailNum = maxFail ? Number(maxFail) : 0;

  console.log(` [x] Received ${topic}`);

  await delay(durationInSecsNum * 1000);

  if (roundToExecute <= maxFailNum) {
    console.log(" [x] Fail");
    throw Error("Fail to execute");
  }

  console.log(" [x] Done");
}

amqp.connect("amqp://localhost", function (error0, connection) {
  if (error0) {
    throw error0;
  }
  connection.createChannel(function (error1, channel) {
    if (error1) {
      throw error1;
    }

    const queue = "task_queue";
    const dlxExchange = "dlx_exchange";
    const dlxKey = "dlx_key";

    // Assert Main Queue with DLX and TTL settings
    channel.assertQueue(queue, {
      durable: true,
      arguments: {
        "x-dead-letter-exchange": dlxExchange,
        "x-dead-letter-routing-key": dlxKey,
        "x-message-ttl": 20000,
      },
    });

    channel.prefetch(1);
    console.log(" [*] Waiting for messages in %s. To exit press CTRL+C", queue);
    channel.consume(
      queue,
      async function (msg) {
        const headers = msg.properties.headers || {};
        const retryCount = headers["x-retry-count"] || 0;
        const content = msg.content.toString();

        try {
          await messageHandler(content, retryCount + 1);
          channel.ack(msg);
        } catch (err) {
          console.log(`Retry count : ${retryCount}`);
          console.error(err);
          if (retryCount < 5) {
            channel.ack(msg);
            headers["x-retry-count"] = retryCount + 1;
            channel.sendToQueue("task_queue", Buffer.from(content), {
              headers: headers,
              persistent: true,
            });
          } else {
            console.log("Reach of max retry!");
            channel.reject(msg, false);
          }
        }
      },
      {
        noAck: false,
      }
    );
  });
});
