package cfrouter.client.impl;

import nats.client.Message;
import nats.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingDeque;

/**
*  Stores received nats msg in a blocking queue to allow easier synchronous interactions
*/
public class RecordingMessageHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(RecordingMessageHandler.class);

    private final String msg;
    private final BlockingDeque<Message> receivedMsgs;

    public RecordingMessageHandler(String msg, BlockingDeque<Message> receivedMsgs) {
        this.msg = msg;
        this.receivedMsgs = receivedMsgs;
    }

    @Override
    public void onMessage(Message message) {
        logger.info(msg + " received with body: " + message.getBody());
        receivedMsgs.add(message);
    }
}
