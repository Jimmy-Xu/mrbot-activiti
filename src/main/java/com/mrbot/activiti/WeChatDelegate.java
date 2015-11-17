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

import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WeChatDelegate implements JavaDelegate {

	private final Logger log = Logger.getLogger(WeChatDelegate.class
			.getSimpleName());
	String className = "WeChatDelegate";
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String curActivityId = "";

	private void write_log(String message) {
		System.out.println(String.format("[%s] - %s() - %s: %s",
				df.format(Calendar.getInstance().getTime()), className,
				curActivityId, message));
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
			System.out.println("\n");
		}
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// http://activiti.org/javadocs/org/activiti/engine/delegate/DelegateExecution.html
		
		String activityId = execution.getCurrentActivityId();
		String activityName = execution.getCurrentActivityName();
		this.curActivityId = activityId;

		show_title(String.format(
				"��ʼִ�ж�ʱ����(execution:%s)=> %s(%s)",
				execution.getId(), activityName, activityId),
				true
		);

		String[] allow_list = { "servicetask-remind", "servicetask-confirm" };
		if (Arrays.asList(allow_list).contains(activityId)) {
			try {
				// ֱ�ӻ�ȡ��������б���
				// System.out.println(String.format("\n��ǰʱ��: %s\n�յ�����: %s",
				// df.format(now.getTime()), execution.getVariables()));

				Map<String, Object> var_map = execution.getVariables();
				write_log(String.format("�յ�%d����������:", var_map.size()));
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

				write_log("������������");
				// ������������
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
				// ��������
				String raw_duetime = execution.getVariable("raw_duetime")
						.toString();
				String wechat_token = execution.getVariable("wechat_token")
						.toString();
				String wechat_api = execution.getVariable("wechat_api")
						.toString();
				@SuppressWarnings({ "unchecked" })
				LinkedHashMap<String, Object> content = (LinkedHashMap<String, Object>) execution
						.getVariable("content");
				// sender and receiver info
				String sender_name = execution.getVariable("sender_name")
						.toString();
				@SuppressWarnings("rawtypes")
				ArrayList receiver_detail = (ArrayList) execution
						.getVariable("receiver_detail");
				@SuppressWarnings("rawtypes")
				Iterator receiver_list = receiver_detail.iterator();

				// write_log("����remind_time");
				//����ʱ��
				DateFormat _df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				_df.setTimeZone(TimeZone.getTimeZone("GMT+8"));
				String remind_time = _df.format(Calendar.getInstance().getTime());
				// write_log("����remind_count");
				long remind_count = Long.parseLong(execution.getVariable(
						"remind_count").toString());
				remind_count++;

				write_log("������������������");
				int total_to_send = receiver_detail.size();
				int index = 0;
				while (receiver_list.hasNext()) {
					index++;
					// ׼������
					@SuppressWarnings("unchecked")
					Map<String, Object> receiver = (Map<String, Object>) receiver_list
							.next();
					String alias = receiver.get("alias").toString();
					String uid = receiver.get("uid").toString();
					String open_id = receiver.get("open_id").toString();
					write_log("[Receiver] alias: " + alias + ", uid: " + uid
							+ ", open_id: " + open_id);

					// ���ɴ�post������
					@SuppressWarnings("unchecked")
					Map<String, Object> keyword2 = (Map<String, Object>) content
							.get("keyword2");
					keyword2.put("value", String.format("%s(%s,��%d��)",
							raw_duetime, remind_time, remind_count));

					Map<String, Object> post_map = null;
					post_map = new HashMap<String, Object>();
					post_map.put("content", content);
					post_map.put("type", activityId);
					post_map.put("open_id", open_id);

					// mapתjson string
					ObjectMapper mapper = new ObjectMapper();
					String data = mapper.writeValueAsString(post_map);
					// write_log(String.format("��post��΢�ŵ�����: %s", data));

					// ����΢��api
					try {
						RESTUtil restUtil = new RESTUtil();
						String result = restUtil.post(wechat_api, data,
								wechat_token);
						write_log("(" + index + "/" + total_to_send
								+ ")[����΢��api�ɹ�](" + sender_name + "->" + alias
								+ "): " + result);
					} catch (Exception e) {
						write_log("(" + index + "/" + total_to_send
								+ ")[����΢��api�쳣](" + sender_name + "->" + alias
								+ "): " + e.getMessage());
						continue;
					}
				}
				//���Ѽ�¼
				execution.setVariable("remind_time", remind_time);
				execution.setVariable("remind_count", remind_count);
				write_log(String.format(
						"[remind_time]:%s [remind_count]:%s", execution
								.getVariable("remind_time").toString(),
						execution.getVariable("remind_count").toString()));
			} catch (Exception e) {
				write_log(String.format("���� %s ʱ����\n����: %s\n��������: %s",
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
			System.out
					.println(String
							.format("\n----------------------------\nδ֪activity�� %s(%s)\n----------------------------",
									activityId, activityName));
		}
		show_title(String.format(
				"����ִ�ж�ʱ����(execution:%s)=> %s(%s)",
				execution.getId(), activityId, activityName),
				false
		);
	}
}
