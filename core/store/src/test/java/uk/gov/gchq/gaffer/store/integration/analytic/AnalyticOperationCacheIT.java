package uk.gov.gchq.gaffer.store.integration.analytic;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.gov.gchq.gaffer.cache.CacheServiceLoader;
import uk.gov.gchq.gaffer.cache.exception.CacheOperationException;
import uk.gov.gchq.gaffer.cache.impl.HashMapCacheService;
import uk.gov.gchq.gaffer.cache.util.CacheProperties;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.analytic.AddAnalyticOperation;
import uk.gov.gchq.gaffer.operation.analytic.AnalyticOperationDetail;
import uk.gov.gchq.gaffer.operation.analytic.GetAllAnalyticOperations;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.StoreProperties;
import uk.gov.gchq.gaffer.store.operation.handler.analytic.AddAnalyticOperationHandler;
import uk.gov.gchq.gaffer.store.operation.handler.analytic.GetAllAnalyticOperationHandler;
import uk.gov.gchq.gaffer.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class AnalyticOperationCacheIT {
    private static final String CACHE_NAME = "AnalyticOperation";
    private final Properties cacheProps = new Properties();
    private final Store store = mock(Store.class);
    private final String adminAuth = "admin auth";
    private final StoreProperties properties = new StoreProperties();

    private AddAnalyticOperation add = new AddAnalyticOperation.Builder()
            .name("op")
            .description("test operation")
            .operation(new OperationChain.Builder()
                    .first(new GetAllElements.Builder()
                            .build())
                    .build())
            .overwrite()
            .score(0)
            .build();

    private User user = new User();
    private User authorisedUser = new User.Builder().userId("authorisedUser").opAuth("authorised").build();
    private User adminAuthUser = new User.Builder().userId("adminAuthUser").opAuth(adminAuth).build();
    private Context context = new Context(user);
    private GetAllAnalyticOperationHandler getAllAnalyticOperationHandler = new GetAllAnalyticOperationHandler();
    private AddAnalyticOperationHandler addAnalyticOperationHandler = new AddAnalyticOperationHandler();
    //private GetAllAnalyticOperationHandler getAllAnalyticOperationHandler1 = new GetAllAnalyticOperationHandler();
    private GetAllAnalyticOperations get = new GetAllAnalyticOperations();

    @Before
    public void before() throws CacheOperationException {
        cacheProps.clear();
        properties.setAdminAuth(adminAuth);
        given(store.getProperties()).willReturn(properties);
    }

    @After
    public void after() throws CacheOperationException {
        CacheServiceLoader.getService().clearCache(CACHE_NAME);
    }

    @Test
    public void shouldWorkUsingHashMapServiceClass() throws OperationException, CacheOperationException {
        reInitialiseCacheService(HashMapCacheService.class);
        runTests();
    }

    private void reInitialiseCacheService(final Class clazz) throws CacheOperationException {
        cacheProps.setProperty(CacheProperties.CACHE_SERVICE_CLASS, clazz.getCanonicalName());
        CacheServiceLoader.initialise(cacheProps);
        CacheServiceLoader.getService().clearCache(CACHE_NAME);
    }

    private void runTests() throws OperationException, CacheOperationException {
        shouldAllowUpdatingOfAnalyticOperations();
        after();
        shouldAllowUpdatingOfAnalyticOperationsWithAllowedUsers();
        after();
        shouldAllowReadingOfAnalyticOperationsUsingAdminAuth();
        after();
        shouldAllowUpdatingOfAnalyticOperationsUsingAdminAuth();
        after();
        shouldBeAbleToAddAnalyticOperationToCache();
        after();
        //shouldBeAbleToDeleteAnalyticOperationFromCache();
    }


    private void shouldBeAbleToAddAnalyticOperationToCache() throws OperationException {
        // given
        GetAllAnalyticOperations get = new GetAllAnalyticOperations.Builder().build();
        final Store store = mock(Store.class);
        given(store.getProperties()).willReturn(properties);

        // when
        addAnalyticOperationHandler.doOperation(add, context, store);

        AnalyticOperationDetail expectedAnalyticOp = new AnalyticOperationDetail.Builder()
                .operationName(add.getOperationName())
                .operation(add.getOperationAsString())
                .creatorId(user.getUserId())
                .readers(new ArrayList<>())
                .writers(new ArrayList<>())
                .description(add.getDescription())
                .score(0)
                .build();

        List<AnalyticOperationDetail> expected = Lists.newArrayList(expectedAnalyticOp);
        List<AnalyticOperationDetail> results = Lists.newArrayList(new GetAllAnalyticOperationHandler().doOperation(get, context, store));

        // then
        assertEquals(1, results.size());
        assertEquals(expected, results);
    }


    /*private void shouldBeAbleToDeleteAnalyticOperationFromCache() throws OperationException {
        // given
        final Store store = mock(Store.class);
        given(store.getProperties()).willReturn(properties);

        new AddAnalyticOperationHandler().doOperation(add, context, store);

        DeleteAnalyticOperation del = new DeleteAnalyticOperation.Builder()
                .name("op")
                .build();

        GetAllAnalyticOperations get = new GetAllAnalyticOperations();

        // when
        deleteAnalyticOperationHandler.doOperation(del, context, store);

        List<AnalyticOperationDetail> results = Lists.newArrayList(getAllAnalyticOperationHandler1.doOperation(get, context, store));

        // then
        assertEquals(0, results.size());

    }*/

    private void shouldAllowUpdatingOfAnalyticOperations() throws OperationException {
        // given
        final Store store = mock(Store.class);
        final StoreProperties storeProps = mock(StoreProperties.class);
        given(store.getProperties()).willReturn(storeProps);

        new AddAnalyticOperationHandler().doOperation(add, context, store);

        AddAnalyticOperation update = new AddAnalyticOperation.Builder()
                .name(add.getOperationName())
                .description("a different operation")
                .operation(add.getOperationAsString())
                .overwrite()
                .score(0)
                .build();

        GetAllAnalyticOperations get = new GetAllAnalyticOperations();

        // when
        new AddAnalyticOperationHandler().doOperation(add, context, store);

        List<AnalyticOperationDetail> results = Lists.newArrayList(getAllAnalyticOperationHandler.doOperation(get, context, store));

        AnalyticOperationDetail expectedAnalyticOp = new AnalyticOperationDetail.Builder()
                .operationName(update.getOperationName())
                .operation(update.getOperationAsString())
                .description(update.getDescription())
                .creatorId(user.getUserId())
                .readers(new ArrayList<>())
                .writers(new ArrayList<>())
                .score(0)
                .build();

        ArrayList<AnalyticOperationDetail> expected = Lists.newArrayList(expectedAnalyticOp);

        // then
        assertEquals(expected.size(), results.size());
        assertEquals(expected, results);
    }

    private void shouldAllowUpdatingOfAnalyticOperationsWithAllowedUsers() throws OperationException {
        // given
        final Store store = mock(Store.class);
        given(store.getProperties()).willReturn(properties);

        new AddAnalyticOperationHandler().doOperation(add, context, store);

        AddAnalyticOperation update = new AddAnalyticOperation.Builder()
                .name(add.getOperationName())
                .description("a different operation")
                .operation(add.getOperationAsString())
                .overwrite()
                .score(0)
                .build();

        GetAllAnalyticOperations get = new GetAllAnalyticOperations();

        // when
        new AddAnalyticOperationHandler().doOperation(add, context, store);

        List<AnalyticOperationDetail> results = Lists.newArrayList(getAllAnalyticOperationHandler.doOperation(get, context, store));

        AnalyticOperationDetail expectedAnalyticOp = new AnalyticOperationDetail.Builder()
                .operationName(update.getOperationName())
                .operation(update.getOperationAsString())
                .description(update.getDescription())
                .creatorId(user.getUserId())
                .readers(new ArrayList<>())
                .writers(new ArrayList<>())
                .score(0)
                .build();

        ArrayList<AnalyticOperationDetail> expected = Lists.newArrayList(expectedAnalyticOp);

        // then
        assertEquals(expected.size(), results.size());
        assertEquals(expected, results);
    }

    private void shouldAllowReadingOfAnalyticOperationsUsingAdminAuth() throws OperationException {
        // given
        Context contextWithAuthorisedUser = new Context(authorisedUser);
        Context contextWithAdminUser = new Context(adminAuthUser);
        AnalyticOperationDetail expectedAnalyticOp = new AnalyticOperationDetail.Builder()
                .operationName(add.getOperationName())
                .operation(add.getOperationAsString())
                .description(add.getDescription())
                .creatorId(authorisedUser.getUserId())
                .readers(new ArrayList<>())
                .writers(new ArrayList<>())
                .score(0)
                .build();
        ArrayList<AnalyticOperationDetail> expected = Lists.newArrayList(expectedAnalyticOp);

        addAnalyticOperationHandler.doOperation(add, contextWithAuthorisedUser, store);

        // when
        List<AnalyticOperationDetail> resultsWithNoAdminRole = Lists.newArrayList(getAllAnalyticOperationHandler.doOperation(get, context, store));

        // then
        assertEquals(0, resultsWithNoAdminRole.size());

        // when
        List<AnalyticOperationDetail> resultsWithAdminRole = Lists.newArrayList(getAllAnalyticOperationHandler.doOperation(get, contextWithAdminUser, store));

        // then
        assertEquals(1, resultsWithAdminRole.size());
        assertEquals(expected, resultsWithAdminRole);
    }

    private void shouldAllowUpdatingOfAnalyticOperationsUsingAdminAuth() throws OperationException {
        // given
        Context contextWithAuthorisedUser = new Context(authorisedUser);
        Context contextWithAdminUser = new Context(adminAuthUser);
        addAnalyticOperationHandler.doOperation(add, contextWithAuthorisedUser, store);

        AddAnalyticOperation update = new AddAnalyticOperation.Builder()
                .name(add.getOperationName())
                .description("a different operation")
                .operation(add.getOperationAsString())
                .overwrite()
                .score(0)
                .build();

        AnalyticOperationDetail expectedAnalyticOp = new AnalyticOperationDetail.Builder()
                .operationName(update.getOperationName())
                .operation(update.getOperationAsString())
                .description(update.getDescription())
                .creatorId(adminAuthUser.getUserId())
                .readers(new ArrayList<>())
                .writers(new ArrayList<>())
                .score(0)
                .build();

        ArrayList<AnalyticOperationDetail> expected = Lists.newArrayList(expectedAnalyticOp);

        // when / then
        try {
            addAnalyticOperationHandler.doOperation(update, context, store);
            fail("Exception expected");
        } catch (final OperationException e) {
            assertTrue(e.getMessage().contains("User UNKNOWN does not have permission to overwrite"));
        }

        // when
        addAnalyticOperationHandler.doOperation(update, contextWithAdminUser, store);

        List<AnalyticOperationDetail> results = Lists.newArrayList(getAllAnalyticOperationHandler.doOperation(get, contextWithAdminUser, store));

        // then
        assertEquals(expected.size(), results.size());
        assertEquals(expected, results);
    }
}
