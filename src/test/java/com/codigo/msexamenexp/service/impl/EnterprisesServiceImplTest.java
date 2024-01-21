package com.codigo.msexamenexp.service.impl;

import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.config.RedisService;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import com.codigo.msexamenexp.feignClient.SunatClient;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import com.codigo.msexamenexp.repository.EnterprisesTypeRespository;
import com.codigo.msexamenexp.util.EnterprisesValidations;
import com.codigo.msexamenexp.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.security.Timestamp;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EnterprisesServiceImplTest {
    @Mock
    private EnterprisesRepository enterprisesRepository;
    @Mock
    private EnterprisesValidations enterprisesValidations;
    @Mock
    private DocumentsTypeRepository typeRepository;
    @Mock
    private EnterprisesTypeRespository enterprisesTypeRespository;
    @Mock
    private Util util;
    @Mock
    private RedisService redisService;
    @Mock
    private SunatClient sunatClient;
    @InjectMocks
    private EnterprisesServiceImpl enterprisesService;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        enterprisesService = new EnterprisesServiceImpl(enterprisesRepository, enterprisesValidations, typeRepository, enterprisesTypeRespository,sunatClient, redisService, util);
    }

    @Test
    void createEnterprise_exitoso() {
        RequestEnterprises requestEnterprises = new RequestEnterprises();
        requestEnterprises.setNumDocument("12345678");
        requestEnterprises.setDocumentsTypeEntity(1);
        requestEnterprises.setEnterprisesTypeEntity(1);
        boolean validate = true;
        EnterprisesTypeEntity enterprisesTypeEntity = new EnterprisesTypeEntity();
        enterprisesTypeEntity.setIdEnterprisesType(1);
        enterprisesTypeEntity.setCodType("1");
        enterprisesTypeEntity.setDescType("SA");
        enterprisesTypeEntity.setStatus(1);
        DocumentsTypeEntity documentsTypeEntity = new DocumentsTypeEntity();
        documentsTypeEntity.setIdDocumentsType(1);
        documentsTypeEntity.setCodType("1");
        documentsTypeEntity.setDescType("DNI");
        documentsTypeEntity.setStatus(1);

        EnterprisesEntity enterprisesEntity = new EnterprisesEntity();
        enterprisesEntity.setIdEnterprises(1);
        enterprisesEntity.setNumDocument("12345678");
        enterprisesEntity.setBusinessName("AENZA S.A.A.");
        enterprisesEntity.setTradeName("AENZA S.A.A.");
        enterprisesEntity.setEnterprisesTypeEntity(enterprisesTypeEntity);
        enterprisesEntity.setDocumentsTypeEntity(documentsTypeEntity);
        enterprisesEntity.setStatus(1);
        ResponseBase responseBaseEsperado = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));
        String redisData = "{\"idEnterprises\":1,\"numDocument\":\"12345678\",\"documentsTypeEntity\":{\"idDocumentsType\":1,\"codType\":\"1\",\"descType\":\"DNI\",\"status\":1},\"enterprisesTypeEntity\":{\"idEnterprisesType\":1,\"codType\":\"1\",\"descType\":\"SA\",\"status\":1},\"status\":1}";
        ResponseSunat responseSunat = new ResponseSunat();
        responseSunat.setRazonSocial("AENZA S.A.A.");
        responseSunat.setNumeroDocumento("12345678");
        Mockito.when(enterprisesValidations.validateInput(requestEnterprises)).thenReturn(validate);
        Mockito.when(enterprisesTypeRespository.save(enterprisesTypeEntity)).thenReturn(enterprisesTypeEntity);
        Mockito.when(typeRepository.save(documentsTypeEntity)).thenReturn(documentsTypeEntity);
        Mockito.when(enterprisesRepository.save(Mockito.any())).thenReturn(enterprisesEntity);
        //Mockito.when(util.convertToJsonEntity(enterprisesEntity)).thenReturn(redisData);
        // mockear el redis metodo void
        Mockito.doNothing().when(redisService).saveKeyValue(Mockito.anyString(),Mockito.anyString(),Mockito.anyInt());
        Mockito.when(sunatClient.getInfoSunat(Mockito.anyString(),Mockito.anyString())).thenReturn(responseSunat);
        ResponseBase responseBaseObtenido = enterprisesService.createEnterprise(requestEnterprises);
        assertEquals(responseBaseEsperado.getCode(), responseBaseObtenido.getCode());
        assertEquals(responseBaseEsperado.getMessage(), responseBaseObtenido.getMessage());
    }
    @Test
    void findOneEnterprise() {
    }

    @Test
    void findAllEnterprises() {
    }

    @Test
    void updateEnterprise() {
    }

    @Test
    void delete() {
    }

    @Test
    void getExecutionSunat() {
    }
}