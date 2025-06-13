package com.vethaa.model;

import java.util.List;

import lombok.Builder;

@Builder
public class AppResponse {

	UserInfo userInfo;
	
	OrgInfo orgInfo;
	
	Object pageData;
	
	List<Message> messages;
	
}
