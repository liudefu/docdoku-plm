/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2017 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.api;

import com.docdoku.api.client.ApiClient;
import com.docdoku.api.client.ApiException;
import com.docdoku.api.models.*;
import com.docdoku.api.services.*;
import org.junit.Assert;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class TestUtils {

    private static final Double MIN_ANGLE = -2 * Math.PI;
    private static final Double MAX_ANGLE = -2 * Math.PI;
    private static final Double MAX_TRANSLATION = -20.0;
    private static final Double MIN_TRANSLATION = 20.0;

    public static String randomString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static Integer randomInt(Integer min, Integer max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    public static AccountDTO createAccount() throws ApiException {
        String login = "USER-" + TestUtils.randomString();
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setLogin(login);
        accountDTO.setEmail(TestConfig.EMAIL);
        accountDTO.setNewPassword(TestConfig.PASSWORD);
        accountDTO.setLanguage(TestConfig.LANGUAGE);
        accountDTO.setName(login);
        accountDTO.setTimeZone(TestConfig.TIMEZONE);
        return new AccountsApi(TestConfig.GUEST_CLIENT).createAccount(accountDTO);
    }

    public static WorkspaceDTO createWorkspace() throws ApiException {
        WorkspaceDTO workspace = new WorkspaceDTO();
        String workspaceId = TestUtils.randomString();
        workspace.setId(workspaceId);
        workspace.setDescription("Generated by tests");
        workspace.setFolderLocked(false);
        WorkspacesApi workspacesApi = new WorkspacesApi(TestConfig.REGULAR_USER_CLIENT);
        return workspacesApi.createWorkspace(workspace, TestConfig.LOGIN);
    }

    public static OrganizationDTO createOrganization() throws ApiException {
        OrganizationDTO organization = new OrganizationDTO();
        String organizationName = TestUtils.randomString();
        organization.setName(organizationName);
        organization.setDescription("Generated by tests");
        OrganizationsApi organizationsApi = new OrganizationsApi(TestConfig.REGULAR_USER_CLIENT);
        return organizationsApi.createOrganization(organization);
    }

    public static Double randomRotation() {
        return MIN_ANGLE + (MAX_ANGLE - MIN_ANGLE) * new Random().nextDouble();
    }

    public static Double randomTranslation() {
        return MIN_TRANSLATION + (MAX_TRANSLATION - MIN_TRANSLATION) * new Random().nextDouble();
    }

    public static PartRevisionDTO createPart(String workspaceId, String partName) throws ApiException {
        PartCreationDTO part = new PartCreationDTO();
        part.setNumber(TestUtils.randomString());
        part.setName(partName);
        return new PartsApi(TestConfig.REGULAR_USER_CLIENT).createNewPart(workspaceId, part);
    }

    public static void assertUserCanEditDocument(ApiClient client, DocumentRevisionDTO document, boolean expect) {
        DocumentApi documentApi = new DocumentApi(client);
        try {
            documentApi.checkOutDocument(document.getWorkspaceId(), document.getDocumentMasterId(), document.getVersion());
            Assert.assertTrue(expect);
            documentApi.undoCheckOutDocument(document.getWorkspaceId(), document.getDocumentMasterId(), document.getVersion());
        } catch (ApiException e) {
            Assert.assertFalse(expect);
            Assert.assertEquals(403, e.getCode());
        }
    }

    public static void assertUserCanEditPart(ApiClient client, PartRevisionDTO part, boolean expect) {
        PartApi partApi = new PartApi(client);
        try {
            partApi.checkOut(part.getWorkspaceId(), part.getNumber(), part.getVersion());
            Assert.assertTrue(expect);
            partApi.undoCheckOut(part.getWorkspaceId(), part.getNumber(), part.getVersion());
        } catch (ApiException e) {
            Assert.assertFalse(expect);
            Assert.assertEquals(403, e.getCode());
        }
    }

    public static void assertUserCanRetrieveDocument(ApiClient client, DocumentRevisionDTO document, boolean expect) {

        // Unit access
        try {
            new DocumentApi(client).getDocumentRevision(document.getWorkspaceId(), document.getDocumentMasterId(), document.getVersion());
            Assert.assertTrue(expect);
        } catch (ApiException e) {
            Assert.assertEquals(403, e.getCode());
            Assert.assertFalse(expect);
        }
        // From lists access
        try {
            List<DocumentRevisionDTO> documentsInWorkspace = new DocumentsApi(client)
                    .getDocumentsInWorkspace(document.getWorkspaceId(), 0, 100);

            DocumentRevisionDTO documentRevisionDTO = documentsInWorkspace
                    .stream()
                    .filter(d -> d.getDocumentMasterId().equals(document.getDocumentMasterId()))
                    .findFirst().orElse(null);

            if (expect) {
                Assert.assertNotNull(documentRevisionDTO);
            } else {
                Assert.assertNull(documentRevisionDTO);
            }

        } catch (ApiException e) {

        }

    }


    public static void assertUserCanRetrievePart(ApiClient client, PartRevisionDTO part, boolean expect) {

        // Unit access
        try {
            new PartApi(client).getPartRevision(part.getWorkspaceId(), part.getNumber(), part.getVersion());
            Assert.assertTrue(expect);
        } catch (ApiException e) {
            Assert.assertEquals(403, e.getCode());
            Assert.assertFalse(expect);
        }
        // From lists access
        try {
            List<PartRevisionDTO> parts = new PartsApi(client)
                    .getPartRevisions(part.getWorkspaceId(), 0, 100);
            PartRevisionDTO partRevisionDTO = parts
                    .stream()
                    .filter(p -> p.getNumber().equals(part.getNumber()))
                    .findFirst().orElse(null);

            if (expect) {
                Assert.assertNotNull(partRevisionDTO);
            } else {
                Assert.assertNull(partRevisionDTO);
            }

        } catch (ApiException e) {

        }

    }

    public static DocumentRevisionDTO createDocument(String workspaceId, List<ACLEntryDTO> userEntries, List<ACLEntryDTO> groupEntries) throws ApiException {
        DocumentCreationDTO document = new DocumentCreationDTO();
        document.setReference(TestUtils.randomString());
        document.setAcl(createACL(userEntries, groupEntries));
        DocumentRevisionDTO createdDocument = new FoldersApi(TestConfig.REGULAR_USER_CLIENT).createDocumentMasterInFolder(workspaceId, document, workspaceId);
        return new DocumentApi(TestConfig.REGULAR_USER_CLIENT).checkInDocument(workspaceId, createdDocument.getDocumentMasterId(), createdDocument.getVersion());
    }

    public static PartRevisionDTO createPart(String workspaceId, List<ACLEntryDTO> userEntries, List<ACLEntryDTO> groupEntries) throws ApiException {
        PartCreationDTO part = new PartCreationDTO();
        part.setNumber(TestUtils.randomString());
        part.setAcl(createACL(userEntries, groupEntries));
        PartRevisionDTO createdPart = new PartsApi(TestConfig.REGULAR_USER_CLIENT).createNewPart(workspaceId, part);
        return new PartApi(TestConfig.REGULAR_USER_CLIENT).checkIn(workspaceId, createdPart.getNumber(), createdPart.getVersion());
    }

    private static ACLDTO createACL(List<ACLEntryDTO> userEntries, List<ACLEntryDTO> groupEntries) {

        ACLDTO acl = new ACLDTO();

        if (null != userEntries) {
            acl.getUserEntries().addAll(userEntries);
        }

        if (null != groupEntries) {
            acl.getGroupEntries().addAll(groupEntries);
        }

        return acl;
    }

}
