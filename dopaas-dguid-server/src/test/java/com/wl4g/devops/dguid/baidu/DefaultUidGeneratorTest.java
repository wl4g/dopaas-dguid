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
package com.wl4g.devops.dguid.baidu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

import com.wl4g.dguid.baidu.UidGenerator;
import com.wl4g.dguid.baidu.impl.DefaultUidGenerator;

/**
 * Test for {@link DefaultUidGenerator}
 * 
 * @author yutianbao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/test/resources/uid/default-uid-baidu.xml" })
public class DefaultUidGeneratorTest {
	private static final int SIZE = 100000; // 10w
	private static final boolean VERBOSE = true;
	private static final int THREADS = Runtime.getRuntime().availableProcessors() << 1;

	@Resource
	private UidGenerator uidGenerator;

	/**
	 * Test for serially generate
	 */
	@Test
	public void testSerialGenerate() {
		// Generate UID serially
		Set<Long> uidSet = new HashSet<>(SIZE);
		for (int i = 0; i < SIZE; i++) {
			doGenerate(uidSet, i);
		}

		// Check UIDs are all unique
		checkUniqueID(uidSet);
	}

	/**
	 * Test for parallel generate
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testParallelGenerate() throws InterruptedException {
		final AtomicInteger control = new AtomicInteger(-1);
		final Set<Long> uidSet = new ConcurrentSkipListSet<>();

		// Initialize threads
		List<Thread> threadList = new ArrayList<>(THREADS);
		for (int i = 0; i < THREADS; i++) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					workerRun(uidSet, control);
				}
			});
			thread.setName("UID-generator-" + i);

			threadList.add(thread);
			thread.start();
		}

		// Wait for worker done
		for (Thread thread : threadList) {
			thread.join();
		}

		// Check generate 10w times
		Assert.assertEquals(SIZE, control.get());

		// Check UIDs are all unique
		checkUniqueID(uidSet);
	}

	public int updateAndGet(AtomicInteger control) {
		int prev, next;
		do {
			prev = control.get();
			next = prev == SIZE ? SIZE : prev + 1;
		} while (!control.compareAndSet(prev, next));
		return next;
	}

	/**
	 * Worker run
	 */
	private void workerRun(Set<Long> uidSet, AtomicInteger control) {
		for (;;) {
			int myPosition = updateAndGet(control);
			if (myPosition == SIZE) {
				return;
			}

			doGenerate(uidSet, myPosition);
		}
	}

	/**
	 * Do generating
	 */
	private void doGenerate(Set<Long> uidSet, int index) {
		long uid = uidGenerator.getUID();
		String parsedInfo = uidGenerator.parseUID(uid);
		uidSet.add(uid);

		// Check UID is positive, and can be parsed
		Assert.assertTrue(uid > 0L);
		Assert.assertTrue(!StringUtils.isEmpty(parsedInfo));

		if (VERBOSE) {
			System.out.println(Thread.currentThread().getName() + " No." + index + " >>> " + parsedInfo);
		}
	}

	/**
	 * Check UIDs are all unique
	 */
	private void checkUniqueID(Set<Long> uidSet) {
		System.out.println(uidSet.size());
		Assert.assertEquals(SIZE, uidSet.size());
	}

}