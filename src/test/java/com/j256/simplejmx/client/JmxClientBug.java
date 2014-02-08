package com.j256.simplejmx.client;

import com.j256.simplejmx.common.ObjectNameUtil;
import com.j256.simplejmx.server.JmxServer;
import org.junit.*;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Created with IntelliJ IDEA.
 * User: michael.ottati
 * Date: 2/7/14
 * Time: 6:09 PM
 *
 * The purpose of this test is to point out an error in the simplejmx.client client 1.6 (and some previous versions as
 * well)
 *
 * The error being exposed has to do with the fact that the JmxClient caches "operations" (see: the null check)
 *
 * 	private String[] lookupParamTypes(ObjectName objectName, String operName, Object[] params) throws JMException {
 checkClientConnected();
 if (operations == null) {
 try {
 operations = mbeanConn.getMBeanInfo(objectName).getOperations();
 } catch (Exception e) {
 throw createJmException("Cannot get attribute info from " + objectName, e);
 }
 }

 * The assumption that the code above makes is that the second invocation is referent to the same ObjetcName as the previous
 * invocation. If not, then the second caller fails.
 *
 * The "both" test fails because of this caching assumption. It is looking for methods in a previous
 */
public class JmxClientBug {

    private static final int JMX_PORT = 8000;
    private static final String JMX_DOMAIN = "foo.com";

    private static final  ObjectName  CODE_CACHE = toObjectName("java.lang:type=MemoryPool,name=Code Cache");
    private static final  ObjectName  THREADING = toObjectName("java.lang:type=Threading");

    private  JmxServer server;
    private  static String beanName;
    private  static ObjectName objectName;
    private  JmxClient client;
    private  JmxClient closedClient;

    @Before
    public  void beforeClass() throws Exception {
        server = new JmxServer(JMX_PORT);
        server.start();
        beanName = JmxClientBug.class.getSimpleName();
        objectName = ObjectNameUtil.makeObjectName(JMX_DOMAIN, beanName);
        client = new JmxClient(JMX_PORT);
        closedClient = new JmxClient(JMX_PORT);
        closedClient.closeThrow();
    }

    @After
    public  void afterClass() {
        if (client != null) {
            client.close();
            client = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Convenience
     * @param objName
     * @return
     */

    private static ObjectName toObjectName(String objName) {
        ObjectName rval;
        try {
            rval =   new ObjectName(objName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid Management Object name supplied. Fatal error.", e);
        }
        return  rval;
    }

    @Test
    public void resetPeakUsageBean() throws Exception {

        client.invokeOperation(CODE_CACHE,"resetPeakUsage");

    }

    @Test
    public void threading() throws Exception {

        client.invokeOperation(THREADING,"resetPeakThreadCount");

    }

    /*
    This test fails because the second invocation (threading) assumes that the operations of the first method apply.

    java.lang.IllegalArgumentException: Cannot find operation named 'resetPeakThreadCount'
	at com.j256.simplejmx.client.JmxClient.lookupParamTypes(JmxClient.java:454)
	at com.j256.simplejmx.client.JmxClient.invokeOperation(JmxClient.java:370)
	at com.j256.simplejmx.client.JmxClientBug.threading(JmxClientBug.java:99)
	at com.j256.simplejmx.client.JmxClientBug.both(JmxClientBug.java:108)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:44)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:41)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:20)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:28)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:31)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:76)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:193)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:52)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:191)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:42)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:236)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:157)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:77)
	at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:195)
	at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:63)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at com.intellij.rt.execution.application.AppMain.main(AppMain.java:120)
     */
    @Test public void both() throws Exception {
        resetPeakUsageBean();
        threading();
    }

}
