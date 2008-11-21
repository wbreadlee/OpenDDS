package org.opendds.jms;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;

import DDS.DATAWRITER_QOS_DEFAULT;
import DDS.DataWriter;
import DDS.DataWriterQos;
import DDS.DataWriterQosHolder;
import DDS.DurabilityQosPolicyKind;
import DDS.Publisher;
import DDS.Topic;
import OpenDDS.JMS.MessagePayloadDataWriter;
import OpenDDS.JMS.MessagePayloadDataWriterHelper;

/**
 *
 */
public class DataWriterPair {
    private MessagePayloadDataWriter persistentDW;
    private MessagePayloadDataWriter volatileDW;
    private Publisher publisher;

    private DataWriterPair(MessagePayloadDataWriter persistentDW, MessagePayloadDataWriter volatileDW, Publisher publisher) {
        this.persistentDW = persistentDW;
        this.volatileDW = volatileDW;
        this.publisher = publisher;
    }

    public static DataWriterPair fromDestination(SessionImpl session, Destination destination) throws JMSException {
        ConnectionImpl connection = session.getOwningConnection();

        DDS.Topic ddsTopic = extractDDSTopicFromDestination(connection, destination);
        Publisher publisher = connection.getPublisher();

        DataWriterQosHolder persistentQosHolder = new DataWriterQosHolder(DATAWRITER_QOS_DEFAULT.get());
        publisher.get_default_datawriter_qos(persistentQosHolder);
        final DataWriterQos persistentQos = persistentQosHolder.value;
        persistentQos.durability.kind = DurabilityQosPolicyKind.VOLATILE_DURABILITY_QOS; //TODO
        final DataWriter dataWriter = publisher.create_datawriter(ddsTopic, persistentQos, null);
        MessagePayloadDataWriter persistentDW = MessagePayloadDataWriterHelper.narrow(dataWriter);

        DataWriterQosHolder volatileQosHolder = new DataWriterQosHolder(DATAWRITER_QOS_DEFAULT.get());
        publisher.get_default_datawriter_qos(volatileQosHolder);
        final DataWriterQos volatileQos = volatileQosHolder.value;
        volatileQos.durability.kind = DurabilityQosPolicyKind.VOLATILE_DURABILITY_QOS;
        final DataWriter dataWriter2 = publisher.create_datawriter(ddsTopic, volatileQos, null);
        MessagePayloadDataWriter volatileDW = MessagePayloadDataWriterHelper.narrow(dataWriter2);

        return new DataWriterPair(persistentDW, volatileDW, publisher);
    }

    private static Topic extractDDSTopicFromDestination(ConnectionImpl connection, Destination destination) throws JMSException {
        // TODO placeholder, to be elaborated
        TopicImpl topicImpl = (TopicImpl) destination;
        return topicImpl.createDDSTopic(connection);
    }

    public MessagePayloadDataWriter getDataWriter(int deliveryMode) {
        if (deliveryMode == DeliveryMode.PERSISTENT) return persistentDW;
        if (deliveryMode == DeliveryMode.NON_PERSISTENT) return volatileDW;
        throw new IllegalArgumentException("Illegal deliveryMode: " + deliveryMode);
    }

    public void destroy() {
        publisher.delete_datawriter(persistentDW);
        publisher.delete_datawriter(volatileDW);
    }
}
