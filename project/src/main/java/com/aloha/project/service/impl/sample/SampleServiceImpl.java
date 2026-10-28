package com.aloha.project.service.impl.sample;

import org.springframework.stereotype.Service;

import com.aloha.project.domain.Sample;
import com.aloha.project.repository.SampleRepository;
import com.aloha.project.service.impl.BaseServiceImpl;
import com.aloha.project.service.inter.sample.SampleService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SampleServiceImpl extends BaseServiceImpl<Sample, SampleRepository> implements SampleService {

  
}
