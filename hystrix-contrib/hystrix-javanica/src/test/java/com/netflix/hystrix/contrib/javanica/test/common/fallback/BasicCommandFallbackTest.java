package com.netflix.hystrix.contrib.javanica.test.common.fallback;


import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixInvokableInfo;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.command.AsyncResult;
import com.netflix.hystrix.contrib.javanica.test.common.BasicHystrixTest;
import com.netflix.hystrix.contrib.javanica.test.common.domain.User;
import org.apache.commons.lang3.Validate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.netflix.hystrix.contrib.javanica.test.common.CommonUtils.getHystrixCommandByKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class BasicCommandFallbackTest extends BasicHystrixTest {

    private UserService userService;

    protected abstract UserService createUserService();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        userService = createUserService();
    }

    @Test
    public void testGetUserAsyncWithFallback() throws ExecutionException, InterruptedException {
            Future<User> f1 = userService.getUserAsync(" ", "name: ");

            assertEquals("def", f1.get().getName());
            assertEquals(1, HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().size());
            HystrixInvokableInfo<?> command = HystrixRequestLog.getCurrentRequest()
                    .getAllExecutedCommands().iterator().next();
            assertEquals("getUserAsync", command.getCommandKey().name());

            // confirm that 'getUserAsync' command has failed
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.FAILURE));
            // and that fallback waw successful
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.FALLBACK_SUCCESS));

    }

    @Test
    public void testGetUserSyncWithFallback() {
            User u1 = userService.getUserSync(" ", "name: ");

            assertEquals("def", u1.getName());
            assertEquals(1, HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().size());
            HystrixInvokableInfo<?> command = HystrixRequestLog.getCurrentRequest()
                    .getAllExecutedCommands().iterator().next();

            assertEquals("getUserSync", command.getCommandKey().name());
            // confirm that command has failed
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.FAILURE));
            // and that fallback was successful
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.FALLBACK_SUCCESS));

    }


    /**
     * * **************************** *
     * * * TEST FALLBACK COMMANDS * *
     * * **************************** *
     */
    @Test
    public void testGetUserAsyncWithFallbackCommand() throws ExecutionException, InterruptedException {
            Future<User> f1 = userService.getUserAsyncFallbackCommand(" ", "name: ");

            assertEquals("def", f1.get().getName());

            assertEquals(3, HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().size());
            HystrixInvokableInfo<?> getUserAsyncFallbackCommand = getHystrixCommandByKey(
                    "getUserAsyncFallbackCommand");
            com.netflix.hystrix.HystrixInvokableInfo firstFallbackCommand = getHystrixCommandByKey("firstFallbackCommand");
            com.netflix.hystrix.HystrixInvokableInfo secondFallbackCommand = getHystrixCommandByKey("secondFallbackCommand");

            assertEquals("getUserAsyncFallbackCommand", getUserAsyncFallbackCommand.getCommandKey().name());
            // confirm that command has failed
            assertTrue(getUserAsyncFallbackCommand.getExecutionEvents().contains(HystrixEventType.FAILURE));
            // confirm that first fallback has failed
            assertTrue(firstFallbackCommand.getExecutionEvents().contains(HystrixEventType.FAILURE));
            // and that second fallback was successful
            assertTrue(secondFallbackCommand.getExecutionEvents().contains(HystrixEventType.FALLBACK_SUCCESS));
    }

    @Test
    public void testGetUserSyncWithFallbackCommand() {
            User u1 = userService.getUserSyncFallbackCommand(" ", "name: ");

            assertEquals("def", u1.getName());
            assertEquals(3, HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().size());
            HystrixInvokableInfo<?> getUserSyncFallbackCommand = getHystrixCommandByKey(
                    "getUserSyncFallbackCommand");
            com.netflix.hystrix.HystrixInvokableInfo firstFallbackCommand = getHystrixCommandByKey("firstFallbackCommand");
            com.netflix.hystrix.HystrixInvokableInfo secondFallbackCommand = getHystrixCommandByKey("secondFallbackCommand");

            assertEquals("getUserSyncFallbackCommand", getUserSyncFallbackCommand.getCommandKey().name());
            // confirm that command has failed
            assertTrue(getUserSyncFallbackCommand.getExecutionEvents().contains(HystrixEventType.FAILURE));
            // confirm that first fallback has failed
            assertTrue(firstFallbackCommand.getExecutionEvents().contains(HystrixEventType.FAILURE));
            // and that second fallback was successful
            assertTrue(secondFallbackCommand.getExecutionEvents().contains(HystrixEventType.FALLBACK_SUCCESS));
    }

    @Test
    @Ignore // todo #929
    public void testAsyncCommandWithAsyncFallback() throws ExecutionException, InterruptedException {
        User user = userService.asyncCommandWithAsyncFallback("", "").get();
        assertEquals("def", user.getId());
    }

    public static class UserService {

        @HystrixCommand(fallbackMethod = "fallback")
        public Future<User> getUserAsync(final String id, final String name) {
            validate(id, name); // validate logic can be inside and outside of AsyncResult#invoke method
            return new AsyncResult<User>() {
                @Override
                public User invoke() {
                    // validate(id, name); possible put validation logic here, in case of any exception a fallback method will be invoked
                    return new User(id, name + id); // it should be network call
                }
            };
        }

        @HystrixCommand(fallbackMethod = "fallback")
        public User getUserSync(String id, String name) {
            validate(id, name);
            return new User(id, name + id); // it should be network call
        }

        private User fallback(String id, String name) {
            return new User("def", "def");
        }

        @HystrixCommand(fallbackMethod = "firstFallbackCommand")
        public Future<User> getUserAsyncFallbackCommand(final String id, final String name) {
            return new AsyncResult<User>() {
                @Override
                public User invoke() {
                    validate(id, name);
                    return new User(id, name + id); // it should be network call
                }
            };
        }

        @HystrixCommand(fallbackMethod = "firstFallbackCommand")
        public User getUserSyncFallbackCommand(String id, String name) {
            validate(id, name);
            return new User(id, name + id); // it should be network call
        }

        // FALLBACK COMMANDS METHODS:
        // This fallback methods will be processed as hystrix commands

        @HystrixCommand(fallbackMethod = "secondFallbackCommand")
        private User firstFallbackCommand(String id, String name) {
            validate(id, name);
            return new User(id, name + id); // it should be network call
        }

        @HystrixCommand(fallbackMethod = "staticFallback")
        private User secondFallbackCommand(String id, String name) {
            validate(id, name);
            return new User(id, name + id); // it should be network call
        }

        @HystrixCommand(fallbackMethod = "asyncFallbackCommand")
        public Future<User> asyncCommandWithAsyncFallback(final String id, final String name){
            return new AsyncResult<User>() {
                @Override
                public User invoke() {
                    validate(id, name);
                    return new User(id, name + id); // it should be network call
                }
            };
        }

        @HystrixCommand
        public Future<User> asyncFallbackCommand(final String id, final String name){
            return new AsyncResult<User>() {
                @Override
                public User invoke() {
                    return  new User("def", "def"); // it should be network call
                }
            };
        }


        private User staticFallback(String id, String name) {
            return new User("def", "def");
        }

        private void validate(String id, String name) {
            Validate.notBlank(id);
            Validate.notBlank(name);
        }

    }
}
