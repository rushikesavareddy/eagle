/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.alert.engine.publisher.dedup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.eagle.alert.engine.coordinator.PolicyDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamColumn;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.publisher.impl.EventUniq;
import org.junit.Assert;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class DedupCacheTest {

    @Test
    public void testNormal() throws Exception {
        Config config = ConfigFactory.load();
        DedupCache dedupCache = new DedupCache(config, "testPublishment");

        StreamDefinition stream = createStream();
        PolicyDefinition policy = createPolicy(stream.getStreamId(), "testPolicy");

        String[] states = new String[] {"OPEN", "WARN", "CLOSE"};
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            AlertStreamEvent event = createEvent(stream, policy, new Object[] {
                System.currentTimeMillis(), "host1", "testPolicy-host1-01", states[random.nextInt(3)], 0, 0
            });
            HashMap<String, String> dedupFieldValues = new HashMap<String, String>();
            dedupFieldValues.put("alertKey", (String) event.getData()[event.getSchema().getColumnIndex("alertKey")]);
            List<AlertStreamEvent> result = dedupCache.dedup(event,
                new EventUniq(event.getStreamId(), event.getPolicyId(), event.getCreatedTime(), dedupFieldValues),
                "state",
                (String) event.getData()[event.getSchema().getColumnIndex("state")], "closed");
            System.out.println((i + 1) + " >>>> " + ToStringBuilder.reflectionToString(result));
        }

        Assert.assertTrue(true);
    }

    private AlertStreamEvent createEvent(StreamDefinition stream, PolicyDefinition policy, Object[] data) {
        AlertStreamEvent event = new AlertStreamEvent();
        event.setPolicyId(policy.getName());
        event.setSchema(stream);
        event.setStreamId(stream.getStreamId());
        event.setTimestamp(System.currentTimeMillis());
        event.setCreatedTime(System.currentTimeMillis());
        event.setData(data);
        return event;
    }

    private StreamDefinition createStream() {
        StreamDefinition sd = new StreamDefinition();
        StreamColumn tsColumn = new StreamColumn();
        tsColumn.setName("timestamp");
        tsColumn.setType(StreamColumn.Type.LONG);

        StreamColumn hostColumn = new StreamColumn();
        hostColumn.setName("host");
        hostColumn.setType(StreamColumn.Type.STRING);

        StreamColumn alertKeyColumn = new StreamColumn();
        alertKeyColumn.setName("alertKey");
        alertKeyColumn.setType(StreamColumn.Type.STRING);

        StreamColumn stateColumn = new StreamColumn();
        stateColumn.setName("state");
        stateColumn.setType(StreamColumn.Type.STRING);

        // dedupCount, dedupFirstOccurrence

        StreamColumn dedupCountColumn = new StreamColumn();
        dedupCountColumn.setName("dedupCount");
        dedupCountColumn.setType(StreamColumn.Type.LONG);

        StreamColumn dedupFirstOccurrenceColumn = new StreamColumn();
        dedupFirstOccurrenceColumn.setName(DedupCache.DEDUP_FIRST_OCCURRENCE);
        dedupFirstOccurrenceColumn.setType(StreamColumn.Type.LONG);

        sd.setColumns(Arrays.asList(tsColumn, hostColumn, alertKeyColumn, stateColumn, dedupCountColumn, dedupFirstOccurrenceColumn));
        sd.setDataSource("testDatasource");
        sd.setStreamId("testStream");
        sd.setDescription("test stream");
        return sd;
    }

    private PolicyDefinition createPolicy(String streamName, String policyName) {
        PolicyDefinition pd = new PolicyDefinition();
        PolicyDefinition.Definition def = new PolicyDefinition.Definition();
        //expression, something like "PT5S,dynamic,1,host"
        def.setValue("test");
        def.setType("siddhi");
        pd.setDefinition(def);
        pd.setInputStreams(Arrays.asList("inputStream"));
        pd.setOutputStreams(Arrays.asList("outputStream"));
        pd.setName(policyName);
        pd.setDescription(String.format("Test policy for stream %s", streamName));

        StreamPartition sp = new StreamPartition();
        sp.setStreamId(streamName);
        sp.setColumns(Arrays.asList("host"));
        sp.setType(StreamPartition.Type.GROUPBY);
        pd.addPartition(sp);
        return pd;
    }

}
