package eu.xenit.apix.rest.v1.tests;

import eu.xenit.apix.data.NodeRef;
import eu.xenit.apix.node.INodeService;
import eu.xenit.apix.node.NodeAssociation;
import eu.xenit.apix.node.ChildParentAssociation;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.transaction.TransactionService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by kenneth on 17.03.16.
 */
public class CopyNodeTest extends BaseTest {

    private final static Logger logger = LoggerFactory.getLogger(CopyNodeTest.class);

    @Autowired
    INodeService nodeService;

    @Autowired
    @Qualifier("TransactionService")
    TransactionService transactionService;

    @Before
    public void setup() {
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
    }

    @Test
    public void testCopyNode() {
        final NodeRef[] nodeRef = init();
        List<ChildParentAssociation> parentAssociations = this.nodeService.getParentAssociations(nodeRef[0]);
        final ChildParentAssociation primaryParentAssoc = (ChildParentAssociation) parentAssociations.get(0);
        final NodeRef parentRef = primaryParentAssoc.getTarget();

        final String url = makeAlfrescoBaseurl("admin", "admin") + "/apix/v1/nodes";
        final CloseableHttpClient httpclient = HttpClients.createDefault();

        transactionService.getRetryingTransactionHelper()
                .doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Object>() {
                    @Override
                    public Object execute() throws Throwable {
                        HttpPost httppost = new HttpPost(url);
                        String jsonString = json(String.format(
                                "{" +
                                        "\"parent\":\"%s\"," +
                                        "\"copyFrom\":\"%s\"" +
                                        "}"
                                , parentRef.toString(), nodeRef[0].toString()));
                        httppost.setEntity(new StringEntity(jsonString));
                        List<ChildParentAssociation> childAssociations = nodeService.getChildAssociations(parentRef);
                        assertEquals(1, childAssociations.size());

                        try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                            logger.debug(EntityUtils.toString(response.getEntity()));

                            assertEquals(200, response.getStatusLine().getStatusCode());
                        }
                        return null;
                    }
                }, false, true);

        List<ChildParentAssociation> newChildAssocs = nodeService.getChildAssociations(parentRef);
        assertEquals(2, newChildAssocs.size());
    }

    @After
    public void cleanUp() {
        this.removeMainTestFolder();
    }
}
