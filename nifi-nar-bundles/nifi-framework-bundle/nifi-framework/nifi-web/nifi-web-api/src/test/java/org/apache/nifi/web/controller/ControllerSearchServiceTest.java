/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.web.controller;

import org.apache.nifi.authorization.Authorizer;
import org.apache.nifi.authorization.RequestAction;
import org.apache.nifi.authorization.user.NiFiUser;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.controller.ProcessorNode;
import org.apache.nifi.controller.StandardProcessorNode;
import org.apache.nifi.controller.flow.FlowManager;
import org.apache.nifi.groups.ProcessGroup;
import org.apache.nifi.parameter.Parameter;
import org.apache.nifi.parameter.ParameterContext;
import org.apache.nifi.parameter.ParameterContextManager;
import org.apache.nifi.parameter.ParameterDescriptor;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.registry.VariableRegistry;
import org.apache.nifi.registry.flow.StandardVersionControlInformation;
import org.apache.nifi.registry.flow.VersionControlInformation;
import org.apache.nifi.registry.variable.MutableVariableRegistry;
import org.apache.nifi.web.api.dto.search.SearchResultsDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ControllerSearchServiceTest {
    private MutableVariableRegistry variableRegistry;
    private ControllerSearchService service;
    private SearchResultsDTO searchResultsDTO;
    private FlowController flowController;
    private ParameterContextManager parameterContextManager;

    @Before
    public void setUp() {
        variableRegistry = mock(MutableVariableRegistry.class);
        service = new ControllerSearchService();
        searchResultsDTO = new SearchResultsDTO();
        flowController = mock(FlowController.class);

        FlowManager mockFlowManager = mock(FlowManager.class);
        parameterContextManager = mock(ParameterContextManager.class);

        doReturn(mockFlowManager).when(flowController).getFlowManager();
        doReturn(parameterContextManager).when(mockFlowManager).getParameterContextManager();
        service.setFlowController(flowController);
    }

    @Test
    public void testSearchInRootLevelAllAuthorizedNoVersionControl() {
        // root level PG
        final ProcessGroup rootProcessGroup = setupMockedProcessGroup("root", null, true, variableRegistry, null);

        // first level PGs
        final ProcessGroup firstLevelAProcessGroup = setupMockedProcessGroup("firstLevelA", rootProcessGroup, true, variableRegistry, null);
        final ProcessGroup firstLevelBProcessGroup = setupMockedProcessGroup("firstLevelB", rootProcessGroup, true, variableRegistry, null);

        // second level PGs
        final ProcessGroup secondLevelAProcessGroup = setupMockedProcessGroup("secondLevelA", firstLevelAProcessGroup, true, variableRegistry, null);
        final ProcessGroup secondLevelBProcessGroup = setupMockedProcessGroup("secondLevelB", firstLevelBProcessGroup, true, variableRegistry, null);
        // third level PGs
        final ProcessGroup thirdLevelAProcessGroup = setupMockedProcessGroup("thirdLevelA", secondLevelAProcessGroup, true, variableRegistry, null);
        final ProcessGroup thirdLevelBProcessGroup = setupMockedProcessGroup("thirdLevelB", secondLevelAProcessGroup, true, variableRegistry, null);

        // link PGs together
        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(firstLevelAProcessGroup);
                add(firstLevelBProcessGroup);
            }
        }).when(rootProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelAProcessGroup);
            }
        }).when(firstLevelAProcessGroup).getProcessGroups();
        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelBProcessGroup);
            }
        }).when(firstLevelBProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(thirdLevelAProcessGroup);
                add(thirdLevelBProcessGroup);
            }
        }).when(secondLevelAProcessGroup).getProcessGroups();

        // setup processor
        setupMockedProcessor("foobar", rootProcessGroup, true, variableRegistry);

        // perform search
        service.search(searchResultsDTO, "foo", rootProcessGroup);

        assertTrue(searchResultsDTO.getProcessorResults().size() == 1);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getId().equals("foobarId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getId().equals("rootId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getName().equals("root"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup() == null);
    }

    @Test
    public void testSearchInThirdLevelAllAuthorizedNoVersionControl() {
        // root level PG
        final ProcessGroup rootProcessGroup = setupMockedProcessGroup("root", null, true, variableRegistry, null);

        // first level PGs
        final ProcessGroup firstLevelAProcessGroup = setupMockedProcessGroup("firstLevelA", rootProcessGroup, true, variableRegistry, null);
        final ProcessGroup firstLevelBProcessGroup = setupMockedProcessGroup("firstLevelB", rootProcessGroup, true, variableRegistry, null);

        // second level PGs
        final ProcessGroup secondLevelAProcessGroup = setupMockedProcessGroup("secondLevelA", firstLevelAProcessGroup, true, variableRegistry, null);
        final ProcessGroup secondLevelBProcessGroup = setupMockedProcessGroup("secondLevelB", firstLevelBProcessGroup, true, variableRegistry, null);
        // third level PGs
        final ProcessGroup thirdLevelAProcessGroup = setupMockedProcessGroup("thirdLevelA", secondLevelAProcessGroup, true, variableRegistry, null);
        final ProcessGroup thirdLevelBProcessGroup = setupMockedProcessGroup("thirdLevelB", secondLevelAProcessGroup, true, variableRegistry, null);

        // link PGs together
        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(firstLevelAProcessGroup);
                add(firstLevelBProcessGroup);
            }
        }).when(rootProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelAProcessGroup);
            }
        }).when(firstLevelAProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelBProcessGroup);
            }
        }).when(firstLevelBProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(thirdLevelAProcessGroup);
                add(thirdLevelBProcessGroup);
            }
        }).when(secondLevelAProcessGroup).getProcessGroups();

        // setup processor
        setupMockedProcessor("foobar", thirdLevelAProcessGroup, true, variableRegistry);

        // perform search
        service.search(searchResultsDTO, "foo", rootProcessGroup);

        assertTrue(searchResultsDTO.getProcessorResults().size() == 1);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getId().equals("foobarId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getId().equals("thirdLevelAId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getName().equals("thirdLevelA"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup() == null);
    }

    @Test
    public void testSearchInThirdLevelParentNotAuthorizedNoVersionControl() {
        // root level PG
        final ProcessGroup rootProcessGroup = setupMockedProcessGroup("root", null, true, variableRegistry, null);

        // first level PGs
        final ProcessGroup firstLevelAProcessGroup = setupMockedProcessGroup("firstLevelA", rootProcessGroup, true, variableRegistry, null);
        final ProcessGroup firstLevelBProcessGroup = setupMockedProcessGroup("firstLevelB", rootProcessGroup, true, variableRegistry, null);

        // second level PGs
        final ProcessGroup secondLevelAProcessGroup = setupMockedProcessGroup("secondLevelA", firstLevelAProcessGroup, true, variableRegistry, null);
        final ProcessGroup secondLevelBProcessGroup = setupMockedProcessGroup("secondLevelB", firstLevelBProcessGroup, true, variableRegistry, null);
        // third level PGs - not authorized
        final ProcessGroup thirdLevelAProcessGroup = setupMockedProcessGroup("thirdLevelA", secondLevelAProcessGroup, false, variableRegistry, null);
        final ProcessGroup thirdLevelBProcessGroup = setupMockedProcessGroup("thirdLevelB", secondLevelAProcessGroup, false, variableRegistry, null);

        // link PGs together
        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(firstLevelAProcessGroup);
                add(firstLevelBProcessGroup);
            }
        }).when(rootProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelAProcessGroup);
            }
        }).when(firstLevelAProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelBProcessGroup);
            }
        }).when(firstLevelBProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(thirdLevelAProcessGroup);
                add(thirdLevelBProcessGroup);
            }
        }).when(secondLevelAProcessGroup).getProcessGroups();

        // setup processor
        setupMockedProcessor("foobar", thirdLevelAProcessGroup, true, variableRegistry);

        // perform search
        service.search(searchResultsDTO, "foo", rootProcessGroup);

        assertTrue(searchResultsDTO.getProcessorResults().size() == 1);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getId().equals("foobarId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getId().equals("thirdLevelAId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getName() == null);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup() == null);
    }

    @Test
    public void testSearchInThirdLevelParentNotAuthorizedWithVersionControl() {
        // root level PG
        final ProcessGroup rootProcessGroup = setupMockedProcessGroup("root", null, true, variableRegistry, null);

        // first level PGs
        final VersionControlInformation versionControlInformation = setupVC();
        final ProcessGroup firstLevelAProcessGroup = setupMockedProcessGroup("firstLevelA", rootProcessGroup, true, variableRegistry, versionControlInformation);
        final ProcessGroup firstLevelBProcessGroup = setupMockedProcessGroup("firstLevelB", rootProcessGroup, true, variableRegistry, null);

        // second level PGs
        final ProcessGroup secondLevelAProcessGroup = setupMockedProcessGroup("secondLevelA", firstLevelAProcessGroup, true, variableRegistry, null);
        final ProcessGroup secondLevelBProcessGroup = setupMockedProcessGroup("secondLevelB", firstLevelBProcessGroup, true, variableRegistry, null);
        // third level PGs - not authorized
        final ProcessGroup thirdLevelAProcessGroup = setupMockedProcessGroup("thirdLevelA", secondLevelAProcessGroup, false, variableRegistry, null);
        final ProcessGroup thirdLevelBProcessGroup = setupMockedProcessGroup("thirdLevelB", secondLevelAProcessGroup, false, variableRegistry, null);

        // link PGs together
        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(firstLevelAProcessGroup);
                add(firstLevelBProcessGroup);
            }
        }).when(rootProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelAProcessGroup);
            }
        }).when(firstLevelAProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelBProcessGroup);
            }
        }).when(firstLevelBProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(thirdLevelAProcessGroup);
                add(thirdLevelBProcessGroup);
            }
        }).when(secondLevelAProcessGroup).getProcessGroups();

        // setup processor
        setupMockedProcessor("foobar", thirdLevelAProcessGroup, true, variableRegistry);

        // perform search
        service.search(searchResultsDTO, "foo", rootProcessGroup);

        assertTrue(searchResultsDTO.getProcessorResults().size() == 1);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getId().equals("foobarId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getId().equals("thirdLevelAId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getName() == null);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup() != null);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup().getId().equals("firstLevelAId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup().getName().equals("firstLevelA"));
    }

    @Test
    public void testSearchInThirdLevelParentNotAuthorizedWithVersionControlInTheGroup() {
        // root level PG
        final ProcessGroup rootProcessGroup = setupMockedProcessGroup("root", null, true, variableRegistry, null);

        // first level PGs
        final ProcessGroup firstLevelAProcessGroup = setupMockedProcessGroup("firstLevelA", rootProcessGroup, true, variableRegistry, null);
        final ProcessGroup firstLevelBProcessGroup = setupMockedProcessGroup("firstLevelB", rootProcessGroup, true, variableRegistry, null);

        // second level PGs
        final ProcessGroup secondLevelAProcessGroup = setupMockedProcessGroup("secondLevelA", firstLevelAProcessGroup, true, variableRegistry, null);
        final ProcessGroup secondLevelBProcessGroup = setupMockedProcessGroup("secondLevelB", firstLevelBProcessGroup, true, variableRegistry, null);
        // third level PGs - not authorized
        final VersionControlInformation versionControlInformation = setupVC();
        final ProcessGroup thirdLevelAProcessGroup = setupMockedProcessGroup("thirdLevelA", secondLevelAProcessGroup, false, variableRegistry, versionControlInformation);
        final ProcessGroup thirdLevelBProcessGroup = setupMockedProcessGroup("thirdLevelB", secondLevelAProcessGroup, false, variableRegistry, null);

        // link PGs together
        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(firstLevelAProcessGroup);
                add(firstLevelBProcessGroup);
            }
        }).when(rootProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelAProcessGroup);
            }
        }).when(firstLevelAProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(secondLevelBProcessGroup);
            }
        }).when(firstLevelBProcessGroup).getProcessGroups();

        Mockito.doReturn(new HashSet<ProcessGroup>() {
            {
                add(thirdLevelAProcessGroup);
                add(thirdLevelBProcessGroup);
            }
        }).when(secondLevelAProcessGroup).getProcessGroups();

        // setup processor
        setupMockedProcessor("foobar", thirdLevelAProcessGroup, true, variableRegistry);

        // perform search
        service.search(searchResultsDTO, "foo", rootProcessGroup);

        assertTrue(searchResultsDTO.getProcessorResults().size() == 1);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getId().equals("foobarId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getId().equals("thirdLevelAId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getParentGroup().getName() == null);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup() != null);
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup().getId().equals("thirdLevelAId"));
        assertTrue(searchResultsDTO.getProcessorResults().get(0).getVersionedGroup().getName() == null);
    }

    @Test
    public void testSearchParameterContext() {
        final ParameterContext paramContext1 = setupMockedParameterContext("foo", "description for parameter context foo", 1, "foo_param", true);
        final ParameterContext paramContext2 = setupMockedParameterContext("bar", "description for parameter context bar", 2, "bar_param", true);
        final Set<ParameterContext> mockedParameterContexts = new HashSet<ParameterContext>();
        mockedParameterContexts.add(paramContext1);
        mockedParameterContexts.add(paramContext2);

        Mockito.doReturn(mockedParameterContexts).when(parameterContextManager).getParameterContexts();

        service.searchParameters(searchResultsDTO, "foo");

        assertTrue(searchResultsDTO.getParameterContextResults().size() == 1);
        assertTrue(searchResultsDTO.getParameterContextResults().get(0).getId().equals("fooId"));
        assertTrue(searchResultsDTO.getParameterContextResults().get(0).getName().equals("foo"));
        // should have a match for the name, id, description
        assertTrue(searchResultsDTO.getParameterContextResults().get(0).getMatches().size() == 3);

        assertTrue(searchResultsDTO.getParameterResults().size() == 1);

        assertTrue(searchResultsDTO.getParameterResults().get(0).getParentGroup().getId().equals("fooId"));
        assertTrue(searchResultsDTO.getParameterResults().get(0).getName().equals("foo_param_0"));
        // and the parameter name, parameter description, and the parameter value
        assertTrue(searchResultsDTO.getParameterResults().get(0).getMatches().size() == 3);
    }

    @Test
    public void testSearchParameterContextNotAuthorized() {
        final ParameterContext paramContext1 = setupMockedParameterContext("foo", "description for parameter context foo", 1, "foo_param", false);
        final ParameterContext paramContext2 = setupMockedParameterContext("bar", "description for parameter context bar", 2, "bar_param", true);
        final Set<ParameterContext> mockedParameterContexts = new HashSet<ParameterContext>();
        mockedParameterContexts.add(paramContext1);
        mockedParameterContexts.add(paramContext2);

        Mockito.doReturn(mockedParameterContexts).when(parameterContextManager).getParameterContexts();

        service.searchParameters(searchResultsDTO, "foo");

        // the matching parameter context is not readable by the user, so there should not be any results
        assertTrue(searchResultsDTO.getParameterContextResults().size() == 0);
        assertTrue(searchResultsDTO.getParameterResults().size() == 0);
    }

    /**
     * Sets up a mock Parameter Context including isAuthorized()
     * @param name                     name of the parameter context
     * @param description              description of the parameter context
     * @param numberOfParams           number of parameters to include as part of this context
     * @param parameterNamePrefix      a prefix for the parameter names
     * @param authorizedToRead         whether or not the user can read the parameter context
     * @return ParameterContext
     */
    private ParameterContext setupMockedParameterContext(String name, String description, int numberOfParams, String parameterNamePrefix, boolean authorizedToRead) {
        final ParameterContext parameterContext = mock(ParameterContext.class);
        Mockito.doReturn(name + "Id").when(parameterContext).getIdentifier();
        Mockito.doReturn(name).when(parameterContext).getName();
        Mockito.doReturn(description).when(parameterContext).getDescription();
        Mockito.doReturn(authorizedToRead).when(parameterContext).isAuthorized(any(Authorizer.class), eq(RequestAction.READ), any(NiFiUser.class));
        Mockito.doReturn(authorizedToRead).when(parameterContext).isAuthorized(eq(null), eq(RequestAction.READ), eq(null));

        Map<ParameterDescriptor, Parameter> parameters = new HashMap<>();
        for (int i = 0; i < numberOfParams; i++) {
            final ParameterDescriptor descriptor = new ParameterDescriptor.Builder()
                    .name(parameterNamePrefix + "_" + i)
                    .description("Description for " + parameterNamePrefix + "_" + i)
                    .sensitive(false)
                    .build();

            final Parameter param = new Parameter(descriptor, parameterNamePrefix + "_" + i + " value");
            parameters.put(descriptor, param);
        }

        Mockito.doReturn(parameters).when(parameterContext).getParameters();

        return parameterContext;
    }

    /**
     * Mocks Processor including isAuthorized() and its name & id.
     *
     * @param processorName          Desired processor name
     * @param containingProcessGroup The process group
     * @param authorizedToRead       Can the processor data be read?
     * @param variableRegistry       The variable registry
     */
    private static void setupMockedProcessor(final String processorName, final ProcessGroup containingProcessGroup, boolean authorizedToRead, final MutableVariableRegistry variableRegistry) {
        final String processorId = processorName + "Id";
        final Processor processor1 = mock(Processor.class);

        final ProcessorNode processorNode1 = mock(StandardProcessorNode.class);
        Mockito.doReturn(authorizedToRead).when(processorNode1).isAuthorized(AdditionalMatchers.or(any(Authorizer.class), isNull()), eq(RequestAction.READ),
                AdditionalMatchers.or(any(NiFiUser.class), isNull()));
        Mockito.doReturn(variableRegistry).when(processorNode1).getVariableRegistry();
        Mockito.doReturn(processor1).when(processorNode1).getProcessor();
        // set processor node's attributes
        Mockito.doReturn(processorId).when(processorNode1).getIdentifier();
        Mockito.doReturn(Optional.ofNullable(null)).when(processorNode1).getVersionedComponentId(); // not actually searching based on versioned component id
        Mockito.doReturn(processorName).when(processorNode1).getName();

        // assign processor node to its PG
        Mockito.doReturn(new HashSet<ProcessorNode>() {
            {
                add(processorNode1);
            }
        }).when(containingProcessGroup).getProcessors();
    }

    /**
     * Mocks ProcessGroup due to isAuthorized(). The final class StandardProcessGroup can't be used.
     *
     * @param processGroupName Desired process group name
     * @param parent           The parent process group
     * @param authorizedToRead Can the process group data be read?
     * @param variableRegistry The variable registry
     * @param versionControlInformation The version control information
     * @return Mocked process group
     */
    private static ProcessGroup setupMockedProcessGroup(final String processGroupName, final ProcessGroup parent, boolean authorizedToRead, final VariableRegistry variableRegistry,
                                                        final VersionControlInformation versionControlInformation) {
        final String processGroupId = processGroupName + "Id";
        final ProcessGroup processGroup = mock(ProcessGroup.class);

        Mockito.doReturn(processGroupId).when(processGroup).getIdentifier();
        Mockito.doReturn(Optional.ofNullable(null)).when(processGroup).getVersionedComponentId(); // not actually searching based on versioned component id
        Mockito.doReturn(processGroupName).when(processGroup).getName();
        Mockito.doReturn(parent).when(processGroup).getParent();
        Mockito.doReturn(versionControlInformation).when(processGroup).getVersionControlInformation();
        Mockito.doReturn(variableRegistry).when(processGroup).getVariableRegistry();
        Mockito.doReturn(parent == null).when(processGroup).isRootGroup();
        // override process group's access rights
        Mockito.doReturn(authorizedToRead).when(processGroup).isAuthorized(AdditionalMatchers.or(any(Authorizer.class), isNull()), eq(RequestAction.READ),
                AdditionalMatchers.or(any(NiFiUser.class), isNull()));

        return processGroup;
    }

    /**
     * Creates a version control information using dummy attributes.
     *
     * @return Dummy version control information
     */
    private static VersionControlInformation setupVC() {
        final StandardVersionControlInformation.Builder builder = new StandardVersionControlInformation.Builder();
        builder.registryId("regId").bucketId("bucId").flowId("flowId").version(1);

        return builder.build();
    }
}
