package com.flipkart.varadhi.core;


import com.flipkart.varadhi.entities.*;
import com.flipkart.varadhi.spi.services.StorageTopicFactory;

import static com.flipkart.varadhi.entities.MetaStoreEntity.NAME_SEPARATOR;

public class VaradhiTopicFactory {

    private final StorageTopicFactory<StorageTopic> topicFactory;

    //TODO:: This is currently used to provide default value for primary region for the topic being created.
    //This should come from TopicResource a part of Regional/HA/BCP-DR policy. Since those are not available
    //use deploymentRegion as global single primary topic region as a workaround.
    private final String deploymentRegion;

    public VaradhiTopicFactory(StorageTopicFactory<StorageTopic> topicFactory, String deploymentRegion) {
        this.topicFactory = topicFactory;
        this.deploymentRegion = deploymentRegion;
    }

    public VaradhiTopic get(Project project, TopicResource topicResource) {
        VaradhiTopic vt = VaradhiTopic.of(topicResource);
        planDeployment(project, vt);
        return vt;
    }


    private void planDeployment(Project project, VaradhiTopic varadhiTopic) {
        StorageTopic storageTopic =
                topicFactory.getTopic(varadhiTopic.getName(), project, varadhiTopic.getCapacityPolicy());
        // This is likely to change with replicated topics across zones. To be taken care as part of DR.
        String internalTopicName = String.join(NAME_SEPARATOR, varadhiTopic.getName(), deploymentRegion);
        InternalTopic internalTopic = new InternalTopic(
                internalTopicName,
                deploymentRegion,
                TopicState.Producing,
                storageTopic
        );
        varadhiTopic.addInternalTopic(internalTopic);
    }
}
