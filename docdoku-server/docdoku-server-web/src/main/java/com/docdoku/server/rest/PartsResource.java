/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2013 DocDoku SARL
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
package com.docdoku.server.rest;

import com.docdoku.core.common.User;
import com.docdoku.core.common.UserGroup;
import com.docdoku.core.common.Workspace;
import com.docdoku.core.exceptions.ApplicationException;
import com.docdoku.core.product.PartMaster;
import com.docdoku.core.product.PartRevision;
import com.docdoku.core.query.PartSearchQuery;
import com.docdoku.core.security.ACL;
import com.docdoku.core.security.ACLUserEntry;
import com.docdoku.core.security.ACLUserGroupEntry;
import com.docdoku.core.security.UserGroupMapping;
import com.docdoku.core.services.IProductManagerLocal;
import com.docdoku.core.services.IUserManagerLocal;
import com.docdoku.server.rest.dto.*;
import com.docdoku.server.rest.util.SearchQueryParser;
import org.dozer.DozerBeanMapperSingletonWrapper;
import org.dozer.Mapper;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
@DeclareRoles(UserGroupMapping.REGULAR_USER_ROLE_ID)
@RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
public class PartsResource {

    @EJB
    private IProductManagerLocal productService;

    @EJB
    private IUserManagerLocal userManager;

    @EJB
    private PartResource part;

    public PartsResource() {
    }

    private Mapper mapper;

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }

    @Path("{partNumber: [^/].*}-{partVersion:[A-Z]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public PartResource getPart() {
        return part;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PartDTO> getPartRevisions(@PathParam("workspaceId") String workspaceId, @QueryParam("start") int start) {
        try {
            int maxResults = 20;
            List<PartRevision> partRevisions = productService.getPartRevisions(Tools.stripTrailingSlash(workspaceId), start, maxResults);
            List<PartDTO> partDTOs = new ArrayList<PartDTO>();

            for(PartRevision partRevision : partRevisions){
                partDTOs.add(Tools.mapPartRevisionToPartDTO(partRevision));
            }

            return partDTOs;
        } catch (ApplicationException ex) {
            throw new RestApiException(ex.toString(), ex.getMessage());
        }
    }

    @GET
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    public PartCountDTO getPartRevisionCount(@PathParam("workspaceId") String workspaceId) {
        try {
            return new PartCountDTO(productService.getPartRevisionsCount(Tools.stripTrailingSlash(workspaceId)));
        } catch (ApplicationException ex) {
            throw new RestApiException(ex.toString(), ex.getMessage());
        }
    }

    @GET
    @Path("search/{query}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PartDTO> searchPartRevisions(@PathParam("workspaceId") String workspaceId, @PathParam("query") String pStringQuery) {
        try{

            PartSearchQuery partSearchQuery = SearchQueryParser.parsePartStringQuery(workspaceId, pStringQuery);

            List<PartRevision> partRevisions = productService.searchPartRevisions(partSearchQuery);
            List<PartDTO> partDTOs = new ArrayList<PartDTO>();

            for(PartRevision partRevision : partRevisions){
                partDTOs.add(Tools.mapPartRevisionToPartDTO(partRevision));
            }

            return partDTOs;
        } catch (ApplicationException ex) {
            throw new RestApiException(ex.toString(), ex.getMessage());
        }
    }

    @GET
    @Path("numbers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<LightPartMasterDTO> searchPartNumbers(@PathParam("workspaceId") String workspaceId, @QueryParam("q") String q) {
        try {
            List<PartMaster> partMasters = productService.findPartMasters(Tools.stripTrailingSlash(workspaceId), "%" + q + "%", 8);
            List<LightPartMasterDTO> partsMastersDTO = new ArrayList<LightPartMasterDTO>();
            for(PartMaster p : partMasters){
                partsMastersDTO.add(new LightPartMasterDTO(p.getNumber()));
            }
            return partsMastersDTO;
        } catch (ApplicationException ex) {
            throw new RestApiException(ex.toString(), ex.getMessage());
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public PartDTO createNewPart(@PathParam("workspaceId") String workspaceId, PartCreationDTO partCreationDTO){

        try {
            String pWorkflowModelId = partCreationDTO.getWorkflowModelId();
            RoleMappingDTO[] rolesMappingDTO = partCreationDTO.getRoleMapping();

            Map<String, String> roleMappings = new HashMap<String,String>();

            if(rolesMappingDTO != null){
                for(RoleMappingDTO roleMappingDTO : rolesMappingDTO){
                    roleMappings.put(roleMappingDTO.getRoleName(),roleMappingDTO.getUserLogin());
                }
            }

            ACLDTO acl = partCreationDTO.getAcl();
            ACLUserEntry[] userEntries = null;
            ACLUserGroupEntry[] userGroupEntries = null;
            if (acl != null) {
                userEntries = new ACLUserEntry[acl.getUserEntries().size()];
                userGroupEntries = new ACLUserGroupEntry[acl.getGroupEntries().size()];
                int i = 0;
                for (Map.Entry<String, ACL.Permission> entry : acl.getUserEntries().entrySet()) {
                    userEntries[i] = new ACLUserEntry();
                    userEntries[i].setPrincipal(new User(new Workspace(workspaceId), entry.getKey()));
                    userEntries[i++].setPermission(ACL.Permission.valueOf(entry.getValue().name()));
                }
                i = 0;
                for (Map.Entry<String, ACL.Permission> entry : acl.getGroupEntries().entrySet()) {
                    userGroupEntries[i] = new ACLUserGroupEntry();
                    userGroupEntries[i].setPrincipal(new UserGroup(new Workspace(workspaceId), entry.getKey()));
                    userGroupEntries[i++].setPermission(ACL.Permission.valueOf(entry.getValue().name()));
                }
            }

            PartMaster partMaster = productService.createPartMaster(workspaceId, partCreationDTO.getNumber(), partCreationDTO.getName(), partCreationDTO.getDescription(), partCreationDTO.isStandardPart(), pWorkflowModelId, partCreationDTO.getDescription(), partCreationDTO.getTemplateId(), roleMappings, userEntries, userGroupEntries);
            PartDTO partDTO = Tools.mapPartRevisionToPartDTO(partMaster.getLastRevision());
            return partDTO;

        } catch (Exception ex) {
            throw new RestApiException(ex.toString(), ex.getMessage());
        }

    }


}