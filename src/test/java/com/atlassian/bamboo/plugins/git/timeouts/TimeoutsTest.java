package com.atlassian.bamboo.plugins.git.timeouts;


import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.NullBuildLogger;
import com.atlassian.bamboo.plugins.git.GitAbstractTest;
import com.atlassian.bamboo.plugins.git.GitOperationHelper;
import com.atlassian.bamboo.repository.RepositoryException;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * This test class is not intended to be run with other test classes - run it manually when solving timeout-related issues.
 */
@Test(enabled = false)
public class TimeoutsTest extends GitAbstractTest
{
    private Thread servingThread;
    private ServerSocket serverSocket;
    private Collection<Socket> connectedSockets = Collections.synchronizedCollection(new ArrayList<Socket>());

    @BeforeClass
    public void setUp() throws Exception
    {
        Field timeout = GitOperationHelper.class.getDeclaredField("DEFAULT_TRANSFER_TIMEOUT");
        timeout.setAccessible(true);
        timeout.setInt(null, 1);

        serverSocket = new ServerSocket(0);

        servingThread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    for (;;)
                    {
                        connectedSockets.add(serverSocket.accept());
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        };
        servingThread.start();
    }

    @AfterClass
    public void tearDown() throws Exception
    {
        servingThread.stop();
        serverSocket.close();
        for (Socket connectedSocket : connectedSockets)
        {
            connectedSocket.close();
        }
    }

    @Test
    public void testTimeoutIsSufficientToCheckOutBigRepo() throws Exception
    {
        BuildLogger bl = new NullBuildLogger()
        {
            @Override
            public String addBuildLogEntry(String logString)
            {
                System.out.println(logString);
                return null;
            }
        };
        GitOperationHelper helper = new GitOperationHelper(bl);
        String s = helper.obtainLatestRevision("git://git.jetbrains.org/idea/community.git", null, null, null);
        File directory = createTempDirectory();
        System.out.println(directory);
        helper.fetchAndCheckout(directory, "git://git.jetbrains.org/idea/community.git", null, s, null, null);
    }

    @DataProvider
    Object[][] urlsToHang()
    {
        return new String[][] {
                {"ssh://localhost:" + serverSocket.getLocalPort() + "/path/to/repo"},
                {"http://localhost:" + serverSocket.getLocalPort() + "/path/to/repo"},
                {"git://localhost:" + serverSocket.getLocalPort() + "/path/to/repo"},
        };
    }

    @Test(dataProvider = "urlsToHang", expectedExceptions = RepositoryException.class, timeOut = 5000)
    public void testTimeoutOnObtainingLatestRevision(String url) throws Exception
    {
        BuildLogger bl = new NullBuildLogger()
        {
            @Override
            public String addBuildLogEntry(String logString)
            {
                System.out.println(logString);
                return null;
            }
        };
        GitOperationHelper helper = new GitOperationHelper(bl);
        String rev = helper.obtainLatestRevision(url, null, null, null);
    }

    @Test(dataProvider = "urlsToHang", expectedExceptions = RepositoryException.class, timeOut = 5000)
    public void testTimeoutOnFetch(String url) throws Exception
    {
        BuildLogger bl = new NullBuildLogger()
        {
            @Override
            public String addBuildLogEntry(String logString)
            {
                System.out.println(logString);
                return null;
            }
        };
        GitOperationHelper helper = new GitOperationHelper(bl);
        File directory = createTempDirectory();
        String rev = helper.fetchAndCheckout(directory, url, null, null, null, null);
    }

}
