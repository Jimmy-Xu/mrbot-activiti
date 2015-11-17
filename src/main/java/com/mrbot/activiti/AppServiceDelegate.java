package com.mrbot.activiti;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppServiceDelegate implements JavaDelegate {

	private final Logger log = Logger.getLogger(AppServiceDelegate.class
			.getSimpleName());

	String className = "AppServiceDelegate";
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String curActivityId = "";
	String curExecutionId = "";
	String curProcessInstanceId = "";

	private void write_log(String message) {
		System.out.println(String.format("[%s]->%s()->%s->%s->%s: %s",
				df.format(Calendar.getInstance().getTime()), className,
				this.curActivityId, this.curProcessInstanceId, this.curExecutionId,
				message));
	}

	private void show_title(String title, boolean isHead) {
		if (isHead){
			System.out.println("\n");
		}
		System.out
				.println(String
						.format("----------------------------------------------------------------------------------------------------------------\n[%s] - %s(): %s\n----------------------------------------------------------------------------------------------------------------",
								df.format(Calendar.getInstance().getTime()),
								className, title));
		if (!isHead){
			System.out.println("");
		}
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// http://activiti.org/javadocs/org/activiti/engine/delegate/DelegateExecution.html

		Calendar.getInstance().setTimeZone( TimeZone.getTimeZone("GMT+8"));
		
		String activityId = execution.getCurrentActivityId();
		String activityName = execution.getCurrentActivityName();
		
		this.curProcessInstanceId = execution.getProcessInstanceId();
		this.curActivityId = activityId;
		this.curExecutionId = execution.getId();

		show_title(String.format(
				"开始执行定时任务(execution:%s)=> %s(%s)",
				execution.getId(), activityName, activityId),
				true
		);

		String[] allow_list = { "servicetask-finish", "servicetask-log" };
		if (Arrays.asList(allow_list).contains(activityId)) {
			try {
				// 直接获取和输出所有变量
				// System.out.println(String.format("\n当前时间: %s\n收到数据: %s",
				// df.format(now.getTime()), execution.getVariables()));

				Map<String, Object> var_map = execution.getVariables();
				write_log(String.format("收到%d个参数如下:", var_map.size()));
				for (String key : var_map.keySet()) {
					if ("|receiver_name_list|sender_name|tran_id|duetime_type|cycle_time|raw_duetime|remind_time|remind_count|cycle_time|"
							.contains("|" + key + "|")) {
						System.out.println(
							String.format("%-20s=> %s",
								key,
								//execution.getVariable(key).getClass().getName(),
								var_map.get(key)
							)
						);
					}
				}

				write_log("解析输入数据");
				// 解析输入数据
				@SuppressWarnings("unused")
				String duetime_type = execution.getVariable("duetime_type")
						.toString();
				@SuppressWarnings("unused")
				String cycle_time = execution.getVariable("cycle_time")
						.toString();
				@SuppressWarnings("unused")
				String end_time = execution.getVariable("end_time").toString();
				/*
				 * @SuppressWarnings("unused") String content_json =
				 * execution.getVariable("content_json").toString();
				 * 
				 * @SuppressWarnings("unused") String receiver_detail_json =
				 * execution.getVariable("receiver_detail_json").toString();
				 * 
				 * @SuppressWarnings("unused") String receiver_name_list =
				 * execution.getVariable("receiver_name_list").toString();
				 * 
				 * @SuppressWarnings("unused") String sender_uid =
				 * execution.getVariable("sender_uid").toString();
				 */
				// 解析参数
				String appservice_token = execution.getVariable(
						"appservice_token").toString();
				String appservice_api = execution.getVariable("appservice_api")
						.toString();
				@SuppressWarnings({ "unchecked", "unused" })
				LinkedHashMap<String, Object> content = (LinkedHashMap<String, Object>) execution
						.getVariable("content");
				// sender and receiver info
				@SuppressWarnings("unused")
				String sender_name = execution.getVariable("sender_name")
						.toString();
				@SuppressWarnings("rawtypes")
				ArrayList receiver_detail = (ArrayList) execution
						.getVariable("receiver_detail");
				@SuppressWarnings({ "rawtypes", "unused" })
				Iterator receiver_list = receiver_detail.iterator();

				if (activityId.equals("servicetask-finish")) {
					// 准备参数
					String process_instance_id = execution
							.getProcessInstanceId();
					String tran_id = execution.getVariable("tran_id")
							.toString();
					write_log("process_instance_id: "
							+ process_instance_id + ", tran_id: " + tran_id);

					// 生成待post的数据
					Map<String, Object> post_map = null;
					post_map = new HashMap<String, Object>();
					post_map.put("state", "done");

					// map转json string
					ObjectMapper mapper = new ObjectMapper();
					String data = mapper.writeValueAsString(post_map);
					write_log(String.format("待post到AppService的数据: %s", data));

					// 调用AppService api
					try {
						RESTUtil restUtil = new RESTUtil();
						String result = restUtil.put(appservice_api + "/"
								+ process_instance_id + '/' + tran_id, data,
								appservice_token);
						write_log("[调用AppService api成功](" + process_instance_id
								+ "," + tran_id + "): " + result);
					} catch (Exception e) {
						write_log("[调用AppService api异常](" + process_instance_id
								+ "," + tran_id + "): " + e.getMessage());
					}
				} else if (activityId.equals("servicetask-log")) {
					/*
					 * String remind_time =
					 * df.format(Calendar.getInstance().getTime()); long
					 * remind_count = (Long)
					 * execution.getVariable("remind_count");
					 * execution.setVariable("remind_time", remind_time);
					 * execution.setVariable("remind_count", remind_count );
					 */
					write_log(String.format(
							"[remind_time]:%s [remind_count]:%s", execution
									.getVariable("remind_time").toString(),
							execution.getVariable("remind_count").toString()));
				}
			} catch (Exception e) {
				write_log(String.format("处理 %s 时出错\n错误: %s\n输入数据: %s",
						activityId, e.getMessage(), execution.getVariables()));

				StackTraceElement[] trace = e.getStackTrace();
				StackTraceElement ste = trace[0];
				System.err.println("error occurred in method: "
						+ ste.getMethodName());
				System.err.println("                    file: "
						+ ste.getFileName());
				System.err.println("             line number: "
						+ ste.getLineNumber());
				System.out.println("");
			}
		} else {
			write_log(String
					.format("\n----------------------------\n未知activity： %s(%s)\n----------------------------",
							activityId, activityName));
		}
		show_title(String.format(
				"结束执行定时任务(execution:%s)=> %s(%s)",
				execution.getId(), activityId, activityName),
				false
		);
	}

}
