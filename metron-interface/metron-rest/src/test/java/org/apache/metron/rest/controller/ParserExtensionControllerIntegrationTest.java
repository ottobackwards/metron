/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.rest.controller;

import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.service.HdfsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class ParserExtensionControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private HdfsService hdfsService;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private String parserExtUrl = "/api/v1/ext/parsers";
    private String user = "user";
    private String password = "password";
    private String extPath = "./target/remote/extension_contrib_lib/";
    private String fileContents = "file contents";

    @Before
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();

    }
    @Test
    public void testSecurity() throws Exception {
        this.mockMvc.perform(post(parserExtUrl).with(csrf()).contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).content(fileContents))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(get(parserExtUrl))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(delete(parserExtUrl).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void test() throws Exception {
        final File bundle = new File("./target/remote/metron-parser-test-assembly-0.4.0-archive.tar.gz");
        final MockMultipartFile multipartFile = new MockMultipartFile("extensionTgz", new FileInputStream(bundle));

        HashMap<String, String> contentTypeParams = new HashMap<String, String>();
        contentTypeParams.put("boundary", "265001916915724");
        MediaType mediaType = new MediaType("multipart", "form-data", contentTypeParams);

        this.mockMvc.perform(MockMvcRequestBuilders.fileUpload(parserExtUrl).file(multipartFile).with(httpBasic(user,password)).contentType(mediaType))
                .andDo(print())
                .andExpect(status().isOk());
       /*
        this.mockMvc.perform(MockMvcRequestBuilders.fileUpload(parserExtUrl)
                .file("extensionTgz", multipartFile.getBytes())
                .contentType(mediaType)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        this.mockMvc.perform(post(parserExtUrl).with(httpBasic(user,password)).with(csrf())
                .requestAttr("extensionTgz", multipartFile.getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE).content(fileContents))
                .andExpect(status().isCreated());
*/
    }

}
