package com.senyer.senyer_opc.service;

import com.senyer.senyer_opc.persistence.domain.DefaultTags;
import com.senyer.senyer_opc.persistence.mapper.DefaultTagsMapper;
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
public class DefaultTagsServiceImpl extends ServiceImpl<DefaultTagsMapper, DefaultTags> implements DefaultTagsService {

}
