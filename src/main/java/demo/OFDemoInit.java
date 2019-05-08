package demo;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.OrderInfo;
import com.example.SupplierInfo;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAliasType;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.service.ServiceRegistry;
import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.process.ProcessContext;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.model.Task;

/**
 * OFDemoInit
 */
public class OFDemoInit {

    final static private int PROBABILITY = 60;
    private static String processId = "OrderManagement";
    private static Random random = new Random(System.currentTimeMillis());

    // demo.OFDemoInit.initDemo(kcontext);
    public static void initDemo(ProcessContext kcontext) {
        startProcesses(kcontext);
        performTasksRequestOffer(kcontext);
    }

    public static void startProcesses(ProcessContext kcontext) {
        KieRuntime runtime = kcontext.getKieRuntime();

        InputStream res = OFDemoInit.class.getClassLoader().getResourceAsStream("demo/order-info-list.xml");
        XStream xStream = new XStream();
        xStream.setClassLoader(OFDemoInit.class.getClassLoader());
        Collection<OrderInfo> list = (Collection<OrderInfo>) xStream.fromXML(res);

        Map<String, Object> params = new HashMap<>();
        List<Long> processInstanceList = new ArrayList<>(list.size());

        for (OrderInfo orderInfo : list) {
            params.clear();
            params.put("orderInfo", orderInfo);
            ProcessInstance processInstance = runtime.startProcess(processId, params);
            processInstanceList.add(processInstance.getId());
        }

        kcontext.setVariable("processInstanceList", processInstanceList);        
    }

    public static void performTasksRequestOffer(ProcessContext kcontext) {
        RuntimeDataService runtimeDataService = (RuntimeDataService) ServiceRegistry.get()
                .service(ServiceRegistry.RUNTIME_DATA_SERVICE);

        UserTaskService userTaskService = (UserTaskService) ServiceRegistry.get()
                .service(ServiceRegistry.USER_TASK_SERVICE);

        List<Long> processInstanceList = (List<Long>) kcontext.getVariable("processInstanceList");

        for (Long id : processInstanceList) {
            if (random.nextInt(100) > PROBABILITY) {
                processInstanceList.remove(id);
                break;
            }

            List<Long> taskIdList = runtimeDataService.getTasksByProcessInstanceId(id);

            for (Long taskId : taskIdList) {
                userTaskService.claim(taskId, null);
                Task task = userTaskService.getTask(taskId);
                
                Map<String, Object> iomap = userTaskService.getTaskInputContentByTaskId(taskId);
                OrderInfo orderInfo = (OrderInfo) iomap.get("orderInfo");
                orderInfo.setTargetPrice(60 * random.nextInt(10));
                orderInfo.setCategory("basic");
                List<String> suppliers;
                if (random.nextInt(1) == 0)
                    suppliers = Arrays.asList("supplier1", "supplier3");
                else
                    suppliers = Arrays.asList("supplier2", "supplier3");

                orderInfo.setSuppliersList(suppliers);
                String userId = task.getTaskData().getActualOwner().getId();
                userTaskService.start(taskId, userId);
                userTaskService.complete(taskId, userId, iomap);
            }

        }
        kcontext.setVariable("processInstanceList", processInstanceList);
    }

    public static void performTasksPrepareOffer(ProcessContext kcontext) {
        RuntimeDataService runtimeDataService = (RuntimeDataService) ServiceRegistry.get()
                .service(ServiceRegistry.RUNTIME_DATA_SERVICE);

        UserTaskService userTaskService = (UserTaskService) ServiceRegistry.get()
                .service(ServiceRegistry.USER_TASK_SERVICE);

        List<Long> processInstanceList = (List<Long>) kcontext.getVariable("processInstanceList");

        for (Long id : processInstanceList) {
            if (random.nextInt(100) > PROBABILITY) {
                processInstanceList.remove(id);
                break;
            }

            List<Long> taskIdList = runtimeDataService.getTasksByProcessInstanceId(id);

            for (Long taskId : taskIdList) {
                userTaskService.claim(taskId, null);
                Task task = userTaskService.getTask(taskId);
                
                Map<String, Object> iomap = userTaskService.getTaskInputContentByTaskId(taskId);
                OrderInfo orderInfo = (OrderInfo) iomap.get("orderInfo");
                SupplierInfo supplierInfo = new SupplierInfo();
                supplierInfo.setDeliveryDate(new Date(LocalDateTime.now().plusDays(random.nextInt(15)).toEpochSecond(ZoneOffset.UTC)));
                supplierInfo.setOffer(orderInfo.getTargetPrice()+10*random.nextInt(10));
                supplierInfo.setUser((String) iomap.get("supplier"));
                iomap.put("supplierInfo", supplierInfo);
                String userId = task.getTaskData().getActualOwner().getId();
                userTaskService.start(taskId, userId);
                userTaskService.complete(taskId, userId, iomap);
            }

        }
        kcontext.setVariable("processInstanceList", processInstanceList);
    }

    public static void performTasksTest(ProcessContext kcontext) {
        RuntimeDataService runtimeDataService = (RuntimeDataService) ServiceRegistry.get()
                .service(ServiceRegistry.RUNTIME_DATA_SERVICE);

        UserTaskService userTaskService = (UserTaskService) ServiceRegistry.get()
                .service(ServiceRegistry.USER_TASK_SERVICE);

        List<Long> processInstanceList = (List<Long>) kcontext.getVariable("processInstanceList");

        for (Long id : processInstanceList) {
            if (random.nextInt(100) > PROBABILITY)
                break;

            List<Long> taskIdList = runtimeDataService.getTasksByProcessInstanceId(id);

            for (Long taskId : taskIdList) {
                userTaskService.claim(taskId, null);
            }
        }
    }

    public static void main(String[] args) {
        // XStream xStream = new XStream();
        // xStream.setClassLoader(OFDemoInit.class.getClassLoader());
        // List<PerformTask> performTasks = new ArrayList<>();
        // performTasks.add(task);
        // System.out.println(xStream.toXML(performTasks));

        // BeanUtilsBean util = new BeanUtilsBean() {
        // @Override
        // public void copyProperty(Object obj, String name, Object value)
        // throws IllegalAccessException, InvocationTargetException {
        // if (value == null)
        // return;
        // if (value instanceof Integer && ((Integer) value).intValue() == 0)
        // return;
        // if (value instanceof Long && ((Long) value).longValue() == 0)
        // return;
        // if (value instanceof Double && ((Double) value).doubleValue() == 0)
        // return;
        // super.copyProperty(obj, name, value);
        // }
        // };
    }

}