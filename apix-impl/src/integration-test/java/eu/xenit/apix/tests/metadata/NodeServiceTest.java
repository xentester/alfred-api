package eu.xenit.apix.tests.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import eu.xenit.apix.alfresco.ApixToAlfrescoConversion;
import eu.xenit.apix.categories.ICategoryService;
import eu.xenit.apix.data.ContentInputStream;
import eu.xenit.apix.node.ChildParentAssociation;
import eu.xenit.apix.node.INodeService;
import eu.xenit.apix.node.MetadataChanges;
import eu.xenit.apix.node.NodeAssociation;
import eu.xenit.apix.node.NodeMetadata;
import eu.xenit.apix.tests.BaseTest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.ContentDataWithId;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


public class NodeServiceTest extends BaseTest {

    private final static Logger logger = LoggerFactory.getLogger(NodeServiceTest.class);
    private static final String TEXT_MIMETYPE = "text/plain";
    StoreRef alfStoreRef = new StoreRef("workspace", "SpacesStore");
    eu.xenit.apix.data.StoreRef apixStoreRef = new eu.xenit.apix.data.StoreRef("workspace", "SpacesStore");
    @Autowired
    private ApixToAlfrescoConversion c;
    @Autowired
    private INodeService service;
    @Autowired
    private ServiceRegistry serviceRegistry;
    @Autowired
    private ContentService contentService;
    @Autowired
    @Qualifier("NodeService")
    private NodeService alfrescoNodeService;
    @Autowired
    @Qualifier("VersionService")
    private VersionService versionService;
    @Autowired
    @Qualifier("CopyService")
    private CopyService copyService;
    @Autowired
    private Repository repository;
    private Set<NodeRef> roots;
    private ICategoryService categoryService;

    @Before
    public void Setup() {
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
        roots = serviceRegistry.getNodeService().getAllRootNodes(alfStoreRef);
    }

    /**
     * Operational integration test
     */
//    @Test
//    @Ignore
//    public void TestSetMetadata_property() {
//        //TODO: this is a big problem
//        //NodeRef noderef = roots.stream().findFirst().get();
//        final NodeRef noderef = getNodeAtPath("/app:company_home/app:dictionary/cm:Samples/cm:sample.pdf");
//        logger.debug(noderef.toString());
//
//        final MetadataChanges changes = new MetadataChanges();
//        HashMap<String, String[]> props = new HashMap<String, String[]>();
//        props.put("cm:title", new String[]{"THIS IS THE ROOT"});
//        changes.setPropertiesToAdd(props);
//
////        NodeMetadata m = serviceRegistry.getTransactionService().getRetryingTransactionHelper()
////                .doInTransaction(() -> service.setMetadata(c.apix(noderef), changes), false, true);
//
//        NodeMetadata m = serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeMetadata>() {
//            @Override
//            public NodeMetadata execute() throws Throwable {
//                return service.setMetadata(c.apix(noderef), changes);
//            }
//        }, false, true);
//
//        logger.debug(m.toString().replaceAll("\n","<br/>"));
//    }

    /**
     * Operational integration testas
     */
    @Test(expected = Exception.class)
    public void TestFailingGetMetadata() {
        eu.xenit.apix.data.NodeRef invalidNodeRef = new eu.xenit.apix.data.NodeRef("invalidNodeRef");
        service.getMetadata(invalidNodeRef);
    }

    @Test
    public void TestSetMetadata() {
        this.cleanUp();
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testFolder");
        FileInfo testNode = this.createTestNode(testFolder.getNodeRef(), "testNode");
        FileInfo testNode2 = this.createTestNode(testFolder.getNodeRef(), "testNode2");

        try {
            String originalName = (String) this.alfrescoNodeService
                    .getProperty(testNode.getNodeRef(), ContentModel.PROP_NAME);
            Boolean hasVersionableAspect = this.alfrescoNodeService
                    .hasAspect(testNode.getNodeRef(), ContentModel.ASPECT_VERSIONABLE);
            Boolean hasTemporaryAspect = this.alfrescoNodeService
                    .hasAspect(testNode.getNodeRef(), ContentModel.ASPECT_TEMPORARY);
            QName nameType = this.alfrescoNodeService.getType(testNode.getNodeRef());

            assertEquals("testNode", originalName);
            assertTrue(hasVersionableAspect);
            assertTrue(hasTemporaryAspect);
            assertEquals(ContentModel.TYPE_CONTENT.toString(), nameType.toString());

            eu.xenit.apix.data.NodeRef testNodeRef = c.apix(testNode.getNodeRef());
            eu.xenit.apix.data.NodeRef testNodeRef2 = c.apix(testNode2.getNodeRef());

            eu.xenit.apix.data.QName[] aspectsToRemove = new eu.xenit.apix.data.QName[]{
                    c.apix(ContentModel.ASPECT_VERSIONABLE), c.apix(ContentModel.ASPECT_TEMPORARY)};
            Map<eu.xenit.apix.data.QName, String[]> propertiesToSet = new HashMap<>();
            propertiesToSet.put(c.apix(ContentModel.PROP_NAME), new String[]{"newName"});
            propertiesToSet.put(c.apix(ContentModel.PROP_ADDRESSEES), new String[]{"addressee1", "addressee2"});

            MetadataChanges changes = new MetadataChanges(c.apix(ContentModel.TYPE_CONTENT),
                    null, aspectsToRemove, propertiesToSet);

            this.service.setMetadata(testNodeRef, changes);

            String newName = (String) this.alfrescoNodeService
                    .getProperty(testNode.getNodeRef(), ContentModel.PROP_NAME);
            List<String> addressees = (List<String>) this.alfrescoNodeService
                    .getProperty(testNode.getNodeRef(), ContentModel.PROP_ADDRESSEES);
            hasVersionableAspect = this.alfrescoNodeService
                    .hasAspect(testNode.getNodeRef(), ContentModel.ASPECT_VERSIONABLE);
            hasTemporaryAspect = this.alfrescoNodeService
                    .hasAspect(testNode.getNodeRef(), ContentModel.ASPECT_TEMPORARY);
            nameType = this.alfrescoNodeService.getType(testNode.getNodeRef());

            assertEquals("newName", newName);
            assertEquals("Should be multivalue", 2, addressees.size());
            assertTrue(addressees.contains("addressee1") && addressees.contains("addressee2"));
            assertFalse(hasVersionableAspect);
            assertFalse(hasTemporaryAspect);
            assertEquals(ContentModel.TYPE_CONTENT.toString(), nameType.toString());

            eu.xenit.apix.data.QName[] aspectsToAdd = new eu.xenit.apix.data.QName[]{
                    c.apix(ContentModel.ASPECT_VERSIONABLE), c.apix(ContentModel.ASPECT_TEMPORARY)};
            changes = new MetadataChanges(null, aspectsToAdd, null, null);

            this.service.setMetadata(testNodeRef, changes);

            hasVersionableAspect = this.alfrescoNodeService
                    .hasAspect(testNode.getNodeRef(), ContentModel.ASPECT_VERSIONABLE);
            hasTemporaryAspect = this.alfrescoNodeService
                    .hasAspect(testNode.getNodeRef(), ContentModel.ASPECT_TEMPORARY);

            assertTrue(hasVersionableAspect);
            assertTrue(hasTemporaryAspect);
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void TestSetAssociations() {
        this.cleanUp();
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testFolder");
        FileInfo testNode = this.createTestNode(testFolder.getNodeRef(), "testNode");
        FileInfo testNode2 = this.createTestNode(testFolder.getNodeRef(), "testNode2");
        FileInfo testNode3 = this.createTestNode(testFolder.getNodeRef(), "testNode3");

        try {
            String originalName = (String) this.alfrescoNodeService
                    .getProperty(testNode.getNodeRef(), ContentModel.PROP_NAME);
            List<AssociationRef> assocs = this.alfrescoNodeService
                    .getTargetAssocs(testNode.getNodeRef(), RegexQNamePattern.MATCH_ALL);

            assertEquals("testNode", originalName);
            assertTrue(assocs.isEmpty());

            eu.xenit.apix.data.NodeRef testNodeRef = c.apix(testNode.getNodeRef());
            eu.xenit.apix.data.NodeRef testNodeRef2 = c.apix(testNode2.getNodeRef());
            eu.xenit.apix.data.NodeRef testNodeRef3 = c.apix(testNode3.getNodeRef());

            service.createAssociation(testNodeRef, testNodeRef2, c.apix(ContentModel.ASSOC_ORIGINAL));

            assocs = this.alfrescoNodeService.getTargetAssocs(testNode.getNodeRef(), RegexQNamePattern.MATCH_ALL);

            assertFalse(assocs.isEmpty());
            assertEquals(1, assocs.size());
            AssociationRef assoc = assocs.get(0);
            assertEquals(ContentModel.ASSOC_ORIGINAL.toString(), assoc.getTypeQName().toString());
            assertEquals(testNodeRef.toString(), assoc.getSourceRef().toString());
            assertEquals(testNodeRef2.toString(), assoc.getTargetRef().toString());

            service.createAssociation(testNodeRef, testNodeRef2, c.apix(ContentModel.ASSOC_AVATAR));

            service.removeAssociation(testNodeRef, testNodeRef2, c.apix(ContentModel.ASSOC_ORIGINAL));

            assocs = this.alfrescoNodeService.getTargetAssocs(testNode.getNodeRef(), RegexQNamePattern.MATCH_ALL);

            assertFalse(assocs.isEmpty());
            assertEquals(1, assocs.size());
            assoc = assocs.get(0);
            assertEquals(ContentModel.ASSOC_AVATAR.toString(), assoc.getTypeQName().toString());
            assertEquals(testNodeRef.toString(), assoc.getSourceRef().toString());
            assertEquals(testNodeRef2.toString(), assoc.getTargetRef().toString());

            service.createAssociation(testNodeRef, testNodeRef3, c.apix(ContentModel.ASSOC_AVATAR));

            assocs = this.alfrescoNodeService.getTargetAssocs(testNode.getNodeRef(), RegexQNamePattern.MATCH_ALL);
            assertEquals(2, assocs.size());
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void TestGetMetadata() {
//        java.util.List<NodeMetadata> metadatas = service.getMetadata(c.apix(roots.stream().collect(Collectors.toList())));
//        assertTrue(roots.stream().allMatch(p -> metadatas.stream().anyMatch(m -> m.id.equals(p.toString()))));
//        assertTrue(metadatas.stream().allMatch(m -> roots.stream().anyMatch(p -> m.id.equals(p.toString()))));

        NodeMetadata metadatas = service.getMetadata(c.apix(roots.iterator().next()));
        logger.debug(metadatas.toString().replaceAll(",", ",\n"));

        assertEquals(c.apix(roots.iterator().next()), metadatas.id);

        assertTrue(metadatas.properties.containsKey(c.apix(ContentModel.PROP_NAME)));
    }

    private Boolean nodeIsInMetaList(NodeRef node, List<NodeMetadata> metadatas) {
        for (NodeMetadata metadata : metadatas) {
            if (metadata.id.equals(node.toString())) {
                return true;
            }
        }

        return false;
    }


    //@Test
    public void TestLoadMetadata() {
        //NodeRef companyHomeNodeRef = getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        String name = "MyDocWithCat456.txt";
        NodeRef ref = serviceRegistry.getFileFolderService().searchSimple(companyHomeRef, name);
        /*if (ref != null) {
            serviceRegistry.getFileFolderService().delete(ref);
            ref = serviceRegistry.getFileFolderService().searchSimple(companyHomeNodeRef, "MyDocWithCat2.txt");
            if (ref != null) throw new RuntimeException("hello");
        }*/

        if (ref == null) {
            logger.debug("Creating doc");

            org.alfresco.service.cmr.model.FileInfo fi = serviceRegistry.getFileFolderService()
                    .create(companyHomeRef, name, ContentModel.TYPE_CONTENT);

            ref = serviceRegistry.getFileFolderService().searchSimple(companyHomeRef, name);
            if (ref == null) {
                throw new RuntimeException("cannot create doc");
            }
        }

        logger.debug(ref.toString());
//
        org.alfresco.service.cmr.repository.NodeService nService = serviceRegistry.getNodeService();
        NodeRef catRef = getNodeAtPath("/cm:generalclassifiable/cm:Regions/cm:EUROPE");
        logger.debug("catref: " + catRef);
//
        categoryService.classifyNode(c.apix(ref), c.apix(catRef));


    }

    public NodeRef getNodeAtPath(String path) {
        StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
        ResultSet rs = serviceRegistry.getSearchService().query(storeRef, SearchService.LANGUAGE_XPATH, path);
        NodeRef companyHomeNodeRef = null;
        try {
            if (rs.length() == 0) {
                throw new RuntimeException("Didn't find node at: " + path);
            }
            companyHomeNodeRef = rs.getNodeRef(0);
        } finally {
            rs.close();
        }
        return companyHomeNodeRef;
    }

    @Override
    public String toString() {
        return "NodeServiceTest{" +
                "service=" + service +
                ", serviceRegistry=" + serviceRegistry +
                ", roots=" + roots +
                '}';
    }

    @Test
    public void testGetChildAssociations() {
        this.cleanUp();
        //NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testFolder");
        FileInfo testNode = this.createTestNode(testFolder.getNodeRef(), "testNode");
        try {
            List<ChildParentAssociation> childAssocs = this.service
                    .getChildAssociations(c.apix(testFolder.getNodeRef()));
            assertEquals(childAssocs.size(), 1);
            for (NodeAssociation childAssoc : childAssocs) {
                assertEquals(childAssoc.getSource().toString(), c.apix(testFolder.getNodeRef()).toString());
                assertEquals(childAssoc.getTarget().toString(), c.apix(testNode.getNodeRef()).toString());
                assertEquals(childAssoc.getType().toString(), ContentModel.ASSOC_CONTAINS.toString());
            }
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testGetParentAssociations() {
        this.cleanUp();
        //NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testFolder");
        FileInfo testNode = this.createTestNode(testFolder.getNodeRef(), "testNode");
        try {
            List<ChildParentAssociation> parentAssocs = this.service
                    .getParentAssociations(c.apix(testNode.getNodeRef()));
            assertEquals(parentAssocs.size(), 1);
            for (NodeAssociation parentAssoc : parentAssocs) {
                assertEquals(parentAssoc.getSource().toString(), c.apix(testNode.getNodeRef()).toString());
                assertEquals(parentAssoc.getTarget().toString(), c.apix(testFolder.getNodeRef()).toString());
                assertEquals(parentAssoc.getType().toString(), ContentModel.ASSOC_CONTAINS.toString());
            }
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testGetPeerAssociations() {
        this.cleanUp();
        //NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testNode = this.createTestNode(mainTestFolder.getNodeRef(), "testNode");
        try {
            NodeRef copyNodeRef = this.copyService.copyAndRename(testNode.getNodeRef(),
                    mainTestFolder.getNodeRef(),
                    ContentModel.ASSOC_CONTAINS,
                    (QName) null, true);

            List<NodeAssociation> peerAssociations = this.service.getTargetAssociations(c.apix(copyNodeRef));
            assertEquals(peerAssociations.size(), 1);
            for (NodeAssociation peerAssoc : peerAssociations) {
                logger.debug(" Peer assoc source: " + peerAssoc.getSource().toString());
                logger.debug(" Peer assoc target: " + peerAssoc.getTarget().toString());
                logger.debug(" Peer assoc type: " + peerAssoc.getType().toString());
                assertEquals(peerAssoc.getSource().toString(), copyNodeRef.toString());
                assertEquals(peerAssoc.getTarget().toString(), testNode.getNodeRef().toString());
                assertEquals(peerAssoc.getType().toString(), ContentModel.ASSOC_ORIGINAL.toString());
            }
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testCopyNode() {
        this.cleanUp();
        //NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testfolder");
        FileInfo testNode = this.createTestNode(mainTestFolder.getNodeRef(), "testnode");
        try {
            logger.debug("sourceNode: " + c.apix(testNode.getNodeRef()));
            logger.debug("targetNode: " + c.apix(testFolder.getNodeRef()));

            eu.xenit.apix.data.NodeRef copiedNodeRef = this.service.copyNode(c.apix(testNode.getNodeRef()),
                    c.apix(testFolder.getNodeRef()),
                    true);

            assertNotNull(copiedNodeRef);
            assertEquals(this.alfrescoNodeService.getPrimaryParent(c.alfresco(copiedNodeRef)).getParentRef().toString(),
                    testFolder.getNodeRef().toString());
            assertEquals(this.alfrescoNodeService.getPrimaryParent(testNode.getNodeRef()).getParentRef().toString(),
                    mainTestFolder.getNodeRef().toString());
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testMoveNode() {
        this.cleanUp();
        //NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testfolder");
        FileInfo testNode = this.createTestNode(mainTestFolder.getNodeRef(), "testnode");

        try {
            this.service.moveNode(c.apix(testNode.getNodeRef()), c.apix(testFolder.getNodeRef()));
            assertEquals(this.alfrescoNodeService.getPrimaryParent(testNode.getNodeRef()).getParentRef().toString(),
                    testFolder.getNodeRef().toString());
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testCreateNode() {
        this.cleanUp();
        //NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testfolder");
        try {
            eu.xenit.apix.data.NodeRef createdNodeRef = this.service
                    .createNode(c.apix(testFolder.getNodeRef()), "testnode", c.apix(ContentModel.TYPE_CONTENT));
            assertNotNull(createdNodeRef);
            assertEquals(
                    this.alfrescoNodeService.getPrimaryParent(c.alfresco(createdNodeRef)).getParentRef().toString(),
                    testFolder.getNodeRef().toString());
            assertEquals(this.alfrescoNodeService.getType(c.alfresco(createdNodeRef)), ContentModel.TYPE_CONTENT);
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }


    /**
     * Successful creation of a node with custom type containing required property.
     */
    @Test
    public void testCreateNodeCustomTypeWithRequiredProperties() {
        this.cleanUp();
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testfolder");

        Map<eu.xenit.apix.data.QName, String[]> propertiesToSet = new HashMap<>();
        propertiesToSet.put(c.apix(ContentModel.PROP_NAME), new String[]{"newName"});

        // mandatory property to set.
        propertiesToSet.put(new eu.xenit.apix.data.QName("{http://test.apix.xenit.eu/model/content}documentStatus"),
                new String[]{"Draft"});

        // custom type containing the mandatory property.
        eu.xenit.apix.data.QName type = new eu.xenit.apix.data.QName(
                "{http://test.apix.xenit.eu/model/content}withMandatoryPropDocument");

        try {
            eu.xenit.apix.data.NodeRef createdNodeRef = this.service
                    .createNode(c.apix(testFolder.getNodeRef()), propertiesToSet, type, null);
            assertNotNull(createdNodeRef);
            assertEquals(
                    this.alfrescoNodeService.getPrimaryParent(c.alfresco(createdNodeRef)).getParentRef().toString(),
                    testFolder.getNodeRef().toString());
            assertEquals(c.apix(this.alfrescoNodeService.getType(c.alfresco(createdNodeRef))), type);
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    /**
     * Failing creation of a node with custom type containing required property. But the property is not part of the
     * list of properties to create!
     */
    @Test/*(expected = org.apache.http.client.HttpResponseException.class)*/
    public void testCreateNodeCustomTypeWithoutRequiredProperties() {
        final NodeServiceTest self = this;

        try {
            // custom type containing the mandatory property but this property is missing.

            serviceRegistry.getRetryingTransactionHelper()
                    .doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
                        @Override
                        public Void execute() throws Throwable {

                            self.cleanUp();

                            final NodeRef companyHomeRef = repository.getCompanyHome();
                            final Map<eu.xenit.apix.data.QName, String[]> propertiesToSet = new HashMap<>();
                            propertiesToSet
                                    .put(c.apix(ContentModel.PROP_NAME), new String[]{"newNameWithoutMandatory"});
                            final eu.xenit.apix.data.QName type = new eu.xenit.apix.data.QName(
                                    "{http://test.apix.xenit.eu/model/content}withMandatoryPropDocument");

                            FileInfo mainTestFolder = self.createMainTestFolder(companyHomeRef);
                            FileInfo testFolder = self.createTestFolder(mainTestFolder.getNodeRef(), "testfolder2");
                            self.service.createNode(c.apix(testFolder.getNodeRef()), propertiesToSet, type, null);
                            return null;
                        }
                    }, false, true);

            fail();
        } catch (org.alfresco.repo.node.integrity.IntegrityException e) {
            assertTrue(true);
        } finally {
            // this hides the raise of exception !!! Why ??
            //self.removeTestNode(mainTestFolder.getNodeRef());
        }


    }


    @Test
    public void testCreateReadContent() throws Exception {
        this.cleanUp();
        NodeRef companyHomeRef = repository.getCompanyHome();
        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testFolder = this.createTestFolder(mainTestFolder.getNodeRef(), "testfolder");

        String mimeType = "text/plain";
        String contentStr = "TEST CONTENT";
        InputStream is = new ByteArrayInputStream(contentStr.getBytes("UTF-8"));

        try {
            eu.xenit.apix.data.ContentData content = this.service.createContent(is, mimeType, "UTF-8");
            assertTrue(content.getContentUrl().matches("^store:\\/\\/[\\da-f\\-\\/]*\\.bin$"));
            assertEquals(contentStr.length(), content.getSize());
            assertEquals(mimeType, content.getMimetype());

            // Create node with content.
            Map<eu.xenit.apix.data.QName, String[]> propertiesToSet = new HashMap<>();
            propertiesToSet.put(c.apix(ContentModel.PROP_NAME), new String[]{"nodeWithContent"});
            eu.xenit.apix.data.NodeRef createdNodeRef = this.service
                    .createNode(c.apix(testFolder.getNodeRef()), propertiesToSet, c.apix(ContentModel.TYPE_CONTENT),
                            content);

            // re-read content of the node.
            ContentInputStream readContent = this.service.getContent(createdNodeRef);

            StringWriter writer = new StringWriter();
            IOUtils.copy(readContent.getInputStream(), writer, "UTF-8");
            String resultingStr = writer.toString();

            assertEquals(contentStr, resultingStr);

        } finally {
            this.cleanUp();
        }
    }

    @Test
    public void testCheckoutCheckin() {
        this.cleanUp();
        //NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");
        NodeRef companyHomeRef = repository.getCompanyHome();

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeRef);
        FileInfo testNode = this.createTestNode(mainTestFolder.getNodeRef(), "testnode");
        try {
            assertTrue(this.versionService.isVersioned(testNode.getNodeRef()));

            eu.xenit.apix.data.NodeRef workingCopy = this.service
                    .checkout(c.apix(testNode.getNodeRef()), c.apix(mainTestFolder.getNodeRef()));
            assertTrue(this.alfrescoNodeService.hasAspect(c.alfresco(workingCopy), ContentModel.ASPECT_WORKING_COPY));
            VersionHistory initialVersionHistory = this.versionService.getVersionHistory(testNode.getNodeRef());
            assertNull(initialVersionHistory);
            String versionComment = "this is a comment";
            eu.xenit.apix.data.NodeRef original = this.service.checkin(workingCopy, versionComment, false);
            VersionHistory finalVersionHistory = this.versionService.getVersionHistory(testNode.getNodeRef());
            logger.debug("Final version history: " + finalVersionHistory.getHeadVersion().getVersionLabel());
            assertEquals(finalVersionHistory.getHeadVersion().getVersionLabel(), "0.1");
            assertEquals(finalVersionHistory.getHeadVersion().getDescription(), versionComment);
            assertEquals(finalVersionHistory.getHeadVersion().getVersionType(), VersionType.MINOR);
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testDeleteNode() {
        this.cleanUp();
        NodeRef companyHomeNodeRef = repository.getCompanyHome();
        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeNodeRef);
        FileInfo testNode = this.createTestNode(mainTestFolder.getNodeRef(), "testNode");
        try {
            assertTrue(this.alfrescoNodeService.exists(testNode.getNodeRef()));

            this.service.deleteNode(c.apix(testNode.getNodeRef()));
            assertFalse(this.alfrescoNodeService.exists(testNode.getNodeRef()));
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testCancelCheckout() {
        this.cleanUp();
        NodeRef companyHomeNodeRef = repository.getCompanyHome();
        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeNodeRef);
        FileInfo testNode = this.createTestNode(mainTestFolder.getNodeRef(), "testNode");

        try {
            assertTrue(this.versionService.isVersioned(testNode.getNodeRef()));
            eu.xenit.apix.data.NodeRef workingCopy = this.service
                    .checkout(c.apix(testNode.getNodeRef()), c.apix(mainTestFolder.getNodeRef()));

            assertTrue(this.alfrescoNodeService.exists(c.alfresco(workingCopy)));
            assertTrue(this.alfrescoNodeService.hasAspect(c.alfresco(workingCopy), ContentModel.ASPECT_WORKING_COPY));

            eu.xenit.apix.data.NodeRef original = this.service.cancelCheckout(workingCopy);
            assertEquals(original.toString(), testNode.getNodeRef().toString());
            assertFalse(this.alfrescoNodeService.exists(c.alfresco(workingCopy)));
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void testGetWorkingCopySource() {
        this.cleanUp();
        NodeRef companyHomeNodeRef = repository.getCompanyHome();
        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeNodeRef);
        FileInfo testNode = this.createTestNode(mainTestFolder.getNodeRef(), "testNode");

        try {
            assertTrue(this.versionService.isVersioned(testNode.getNodeRef()));
            eu.xenit.apix.data.NodeRef workingCopy = this.service
                    .checkout(c.apix(testNode.getNodeRef()), c.apix(mainTestFolder.getNodeRef()));

            assertTrue(this.alfrescoNodeService.exists(c.alfresco(workingCopy)));
            assertTrue(this.alfrescoNodeService.hasAspect(c.alfresco(workingCopy), ContentModel.ASPECT_WORKING_COPY));

            eu.xenit.apix.data.NodeRef original = this.service.getWorkingCopySource(workingCopy);
            assertEquals(original.toString(), testNode.getNodeRef().toString());
            this.service.cancelCheckout(workingCopy);
            assertFalse(this.alfrescoNodeService.exists(c.alfresco(workingCopy)));
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }


    @Test
    public void TestFileUpload() throws IOException {
        this.cleanUp();
        NodeRef companyHomeNodeRef = this.getNodeAtPath("/app:company_home");

        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeNodeRef);

        File testFile = new File("test.txt");
        Boolean newFileCreated = testFile.createNewFile();
        PrintWriter writer = new PrintWriter("test.txt", "UTF-8");
        String contentString = "This is the content";
        writer.println(contentString);
        writer.close();

        try {

            InputStream inputStream = new FileInputStream(testFile);

            eu.xenit.apix.data.NodeRef createdNodeRef = this.service
                    .createNode(c.apix(mainTestFolder.getNodeRef()), testFile.getName(),
                            c.apix(ContentModel.TYPE_CONTENT));
            this.service.setContent(createdNodeRef, inputStream, testFile.getName());

            QName createdNodeType = this.alfrescoNodeService.getType(c.alfresco(createdNodeRef));
            String fileContentString = this.contentService
                    .getReader(c.alfresco(createdNodeRef), ContentModel.PROP_CONTENT).getContentString();
            ContentDataWithId contentProperty = (ContentDataWithId) this.alfrescoNodeService
                    .getProperty(c.alfresco(createdNodeRef), ContentModel.PROP_CONTENT);
            String mimeType = contentProperty.getMimetype();
            String name = (String) this.alfrescoNodeService
                    .getProperty(c.alfresco(createdNodeRef), ContentModel.PROP_NAME);

            assertEquals(ContentModel.TYPE_CONTENT.toString(), createdNodeType.toString());
            logger.debug(" contentString: " + contentString);
            logger.debug(" fileContentString: " + fileContentString);
            //assertEquals(contentString.toLowerCase(), fileContentString.toLowerCase());
            assertEquals(TEXT_MIMETYPE, mimeType);
            assertEquals(testFile.getName(), name);
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
            testFile.delete();
        }
    }


    @Test
    public void TestSetContent_ShortOverload() {
        //test for setContent(eu.xenit.apix.data.NodeRef node, eu.xenit.apix.data.ContentData contentData)
        this.cleanUp();
        NodeRef companyHomeNodeRef = repository.getCompanyHome();
        FileInfo mainTestFolder = this.createMainTestFolder(companyHomeNodeRef);
        FileInfo testNodeFI = this.createTestNode(mainTestFolder.getNodeRef(), "testNode");
        NodeService alfNodeService = serviceRegistry.getNodeService();
        NodeRef testNode = testNodeFI.getNodeRef();

        try {
            eu.xenit.apix.data.ContentData apContentData =
                    new eu.xenit.apix.data.ContentData(
                            "www.mycontent.be",
                            "text/plain",
                            128,
                            "UTF8",
                            Locale.CHINA);
            service.setContent(c.apix(testNode), apContentData);
            ContentData newData = (ContentData) alfNodeService.getProperty(testNode, ContentModel.PROP_CONTENT);
            assertTrue(newData.getContentUrl().equals("www.mycontent.be"));
            assertTrue(newData.getMimetype().equals("text/plain"));
            assertTrue(newData.getEncoding().equals("UTF8"));
            assertTrue(newData.getSize() == (128));

            assertTrue(newData.getLocale().equals(Locale.CHINA));
        } finally {
            this.removeTestNode(mainTestFolder.getNodeRef());
        }
    }

    @Test
    public void TestGetRootNode() {
        assertEquals(
                service.getRootNode(apixStoreRef).toString(),
                alfrescoNodeService.getRootNode(alfStoreRef).toString());
    }
}
