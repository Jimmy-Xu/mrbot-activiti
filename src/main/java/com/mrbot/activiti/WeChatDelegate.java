package com.mrbot.activiti;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WeChatDelegate implements JavaDelegate {

	private final Logger log = Logger.getLogger(WeChatDelegate.class.getName());

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// http://activiti.org/javadocs/org/activiti/engine/delegate/DelegateExecution.html

		String activityId = execution.getCurrentActivityId();
		String activityName = execution.getCurrentActivityName();

		log.info(String.format("WeChatDelegate() ��ʱ����ʼִ��: execution id: %s, activityId: %s, activityName: %s "
				, execution.getId(), activityId, activityName ));

		// current time
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
		Calendar now = Calendar.getInstance();
		String[] allow_list = {"servicetask-remind","servicetask-confirm"};
		if ( Arrays.asList(allow_list).contains(activityId) ){
			try{
				log.info(String.format("\n----------------------------\n%s(%s)\n----------------------------",
					activityId, activityName));

				// ֱ�ӻ�ȡ��������б���
				log.info(String.format("\n��ǰʱ��: %s\n�յ�����: %s", df.format(now.getTime()), execution.getVariables()));

				//������������
//				String start_time = execution.getVariable("start_time").toString();
//				String end_time = execution.getVariable("end_time").toString();
//				String sender_uid = execution.getVariable("sender_uid").toString();
//				String sender_name = execution.getVariable("sender_name").toString();
//				String receiver_uid = execution.getVariable("receiver_uid").toString();
//				String receiver_name = execution.getVariable("receiver_name").toString();
				String url = execution.getVariable("url").toString();
				String token = execution.getVariable("token").toString();		//base64 encoded
				String open_id = execution.getVariable("open_id").toString(); 	//open_id in WeChat
				String content = execution.getVariable("content").toString();

				//���ɴ�post������
				ObjectMapper mapper = new ObjectMapper();
				Map<String, Object> map = null;
				map = new HashMap<String, Object>();
				map.put("open_id", open_id);
				map.put("content", execution.getVariable("content"));

				//���type��������post��������
				if (activityId.equals("servicetask-remind")) {	
					map.put("type","remind");
				} else if (activityId.equals("servicetask-confirm")) {
					map.put("type","confirm");
				}

				// mapתjson string
				String data = mapper.writeValueAsString(map);
				log.info(String.format("��post��΢�ŵ�����: %s", df.format(now.getTime()), data));

				// ����΢��api
				try {
					RESTUtil restUtil = new RESTUtil();
					String result = restUtil.post(url, data, token);
					log.info("����΢��api�ɹ�: "+result);
				} catch (Exception e) {
					log.info("����΢��api�쳣: "+e.getMessage());
				}
			}
			catch(Exception e){
				log.info(String.format("���� %s ʱ����: %s\n��������: %s",e.getLocalizedMessage(),execution.getVariables()));
			}
		}
		else{
			log.info(String
					.format("\n----------------------------\nδ֪activity�� %s(%s)\n----------------------------",
							activityId, activityName));
		}
	}
}
