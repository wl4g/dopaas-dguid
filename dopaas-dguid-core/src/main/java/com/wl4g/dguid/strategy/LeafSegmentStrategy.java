/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.dguid.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wl4g.dguid.leaf.DefaultLeafIdSegmentHandler;
import com.wl4g.dguid.leaf.LeafIdSegmentHandler;

/**
 * Leaf分段批量Id策略(可配置asynLoadingSegment - 异步标识)
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2019年2月10日
 * @since
 */
public class LeafSegmentStrategy implements IUidStrategy {

	private final static String MSG_UID_PARSE = "{\"UID\":\"%s\"}";

	/**
	 * 生成器集合
	 */
	protected static Map<String, LeafIdSegmentHandler> generatorMap = new ConcurrentHashMap<>();

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	private boolean asynLoadingSegment = true;

	/**
	 * 获取uid生成器
	 * 
	 * @方法名称 getUidGenerator
	 * @功能描述
	 * 
	 *       <pre>
	 *       获取uid生成器
	 *       </pre>
	 * 
	 * @param prefix
	 *            前缀
	 * @return uid生成器
	 */
	public LeafIdSegmentHandler getSegmentService(String prefix) {
		LeafIdSegmentHandler segmentService = generatorMap.get(prefix);
		if (null == segmentService) {
			synchronized (generatorMap) {
				if (null == segmentService) {
					segmentService = new DefaultLeafIdSegmentHandler(jdbcTemplate, prefix);
				}
				generatorMap.put(prefix, segmentService);
			}
		}
		return segmentService;
	}

	@Override
	public long getUID(String group) {
		return getSegmentService(group).getId();
	}

	@Override
	public String parseUID(long uid, String group) {
		return String.format(MSG_UID_PARSE, uid);
	}

	public boolean isAsynLoadingSegment() {
		return asynLoadingSegment;
	}

	public void setAsynLoadingSegment(boolean asynLoadingSegment) {
		this.asynLoadingSegment = asynLoadingSegment;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}