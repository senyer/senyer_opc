package com.senyer.senyer_opc.service;

import com.senyer.senyer_opc.persistence.domain.OpcProperties;
import com.senyer.senyer_opc.persistence.mapper.OpcPropertiesMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author senyer
 * @since 2020-02-17
 */
@Service
public class OpcPropertiesServiceImpl extends ServiceImpl<OpcPropertiesMapper, OpcProperties> implements OpcPropertiesService {

}
