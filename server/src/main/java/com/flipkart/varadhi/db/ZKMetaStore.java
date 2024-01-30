package com.flipkart.varadhi.db;

import com.flipkart.varadhi.entities.MetaStoreEntity;
import com.flipkart.varadhi.exceptions.DuplicateResourceException;
import com.flipkart.varadhi.exceptions.InvalidOperationForResourceException;
import com.flipkart.varadhi.exceptions.ResourceNotFoundException;
import com.flipkart.varadhi.spi.db.MetaStoreException;
import com.flipkart.varadhi.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.util.List;


@Slf4j
public class ZKMetaStore {
    /*
    Implementation Details: < T extends VaradhiResource>
    Some APIs works on VaradhiResource abstraction. VaradhiResource implements Versioned entities.
    However, for ZKMetaStore, version is of stat object and not data object.
    So there will be a mismatch in data object version and stat object version. Stat object version is a valid version.
    create/get/update APIs overwrites the data version with stat version always.
    Another implication -- manual data update when done, will automatically bump the entity version w/o user being aware of it.
     */
    private final CuratorFramework zkCurator;

    public ZKMetaStore(CuratorFramework zkCurator) {
        this.zkCurator = zkCurator;
    }

    void createZNode(ZNode znode) {
        try {
            String response = zkCurator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
                    .forPath(znode.getPath());
            log.debug("Created zk path {} in ZK: {}", znode, response);
        } catch (Exception e) {
            throw new MetaStoreException(String.format("Failed to create path %s.", znode.getPath()), e);
        }
    }

    <T extends MetaStoreEntity> void createZNodeWithData(ZNode znode, T dataObject) {
        try {
            String jsonData = JsonMapper.jsonSerialize(dataObject);
            String response =
                    zkCurator.create().withMode(CreateMode.PERSISTENT).forPath(znode.getPath(), jsonData.getBytes());
            log.debug("Created znode for {}({}) in at {}: {}.", znode.getKind(), znode.getName(), znode.getPath(),
                    response
            );
            dataObject.setVersion(0);
        } catch (KeeperException.NodeExistsException e) {
            throw new DuplicateResourceException(
                    String.format("%s(%s) already exists.", znode.getKind(), znode.getName()), e);
        } catch (Exception e) {
            throw new MetaStoreException(
                    String.format("Failed to create %s(%s) at %s.", znode.getKind(), znode.getName(),
                            znode.getPath()
                    ), e);
        }
    }

    <T extends MetaStoreEntity> int updateZNodeWithData(ZNode znode, T dataObject) {
        try {
            String jsonData = JsonMapper.jsonSerialize(dataObject);
            Stat stat = zkCurator.setData().withVersion(dataObject.getVersion())
                    .forPath(znode.getPath(), jsonData.getBytes());
            log.debug("Updated {}({}) in at {}: New Version{}.", znode.getKind(), znode.getName(), znode.getPath(),
                    stat.getVersion()
            );
            return stat.getVersion();
        } catch (KeeperException.NoNodeException e) {
            throw new ResourceNotFoundException(
                    String.format("%s(%s) not found.", znode.getKind(), znode.getName()), e);
        } catch (KeeperException.BadVersionException e) {
            throw new InvalidOperationForResourceException(
                    String.format(
                            "Conflicting update, %s(%s) has been modified. Fetch latest and try again. %s",
                            znode.getKind(), znode.getName(), e.getMessage()
                    ), e);
        } catch (Exception e) {
            throw new MetaStoreException(
                    String.format("Failed to update %s(%s) at %s.", znode.getKind(), znode.getName(),
                            znode.getPath()
                    ), e);
        }
    }

    <T extends MetaStoreEntity> T getZNodeDataAsPojo(ZNode znode, Class<T> pojoClazz) {
        byte[] jsonData;
        Stat stat;
        try {
            stat = new Stat();
            jsonData = zkCurator.getData().storingStatIn(stat).forPath(znode.getPath());
        } catch (KeeperException.NoNodeException e) {
            throw new ResourceNotFoundException(
                    String.format("%s(%s) not found.", znode.getKind(), znode.getName()),
                    e
            );
        } catch (Exception e) {
            throw new MetaStoreException(
                    String.format("Failed to find %s(%s) at %s.", znode.getKind(), znode.getName(), znode.getPath()),
                    e
            );
        }
        T resource = JsonMapper.jsonDeserialize(new String(jsonData), pojoClazz);
        resource.setVersion(stat.getVersion());
        return resource;
    }

    boolean zkPathExist(ZNode znode) {
        try {
            return null != zkCurator.checkExists().forPath(znode.getPath());
        } catch (Exception e) {
            throw new MetaStoreException(
                    String.format("Failed to check if %s(%s) exists.", znode.getName(), znode.getPath()), e);
        }
    }

    void deleteZNode(ZNode znode) {
        try {
            zkCurator.delete().forPath(znode.getPath());
        } catch (KeeperException.NoNodeException e) {
            throw new ResourceNotFoundException(
                    String.format("%s(%s) not found.", znode.getKind(), znode.getName()), e);
        } catch (Exception e) {
            throw new MetaStoreException(
                    String.format("Failed to delete %s(%s) at %s.", znode.getKind(), znode.getName(),
                            znode.getPath()
                    ), e);
        }
    }

    List<String> listChildren(ZNode znode) {
        if (!zkPathExist(znode)) {
            throw new ResourceNotFoundException(
                    String.format("Path(%s) not found for entity %s.", znode.getPath(), znode.getName()));
        }
        try {
            return zkCurator.getChildren().forPath(znode.getPath());
        } catch (Exception e) {
            throw new MetaStoreException(
                    String.format("Failed to list children for entity type %s at path %s.", znode.getName(),
                            znode.getPath()
                    ), e);
        }
    }
}
