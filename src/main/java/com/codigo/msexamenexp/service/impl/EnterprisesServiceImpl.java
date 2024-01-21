package com.codigo.msexamenexp.service.impl;

import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.config.RedisService;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import com.codigo.msexamenexp.feignClient.SunatClient;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import com.codigo.msexamenexp.repository.EnterprisesTypeRespository;
import com.codigo.msexamenexp.service.EnterprisesService;
import com.codigo.msexamenexp.util.EnterprisesValidations;
import com.codigo.msexamenexp.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
public class EnterprisesServiceImpl implements EnterprisesService {

    private final EnterprisesRepository enterprisesRepository;
    private final EnterprisesValidations enterprisesValidations;
    private final DocumentsTypeRepository typeRepository;
    private final EnterprisesTypeRespository enterprisesTypeRespository;
    private final Util util;
    private RedisService redisService;
    private final SunatClient sunatClient;
    @Value("${token.api.sunat}")
    private String tokenSunat;
    @Value("${time.expiration.sunat.info}")
    private String timeExpirationSunatInfo;
    public EnterprisesServiceImpl(EnterprisesRepository enterprisesRepository, EnterprisesValidations enterprisesValidations, DocumentsTypeRepository typeRepository, EnterprisesTypeRespository enterprisesTypeRespository, SunatClient sunatClient, RedisService redisService, Util util) {
        this.enterprisesRepository = enterprisesRepository;
        this.enterprisesValidations = enterprisesValidations;
        this.enterprisesTypeRespository = enterprisesTypeRespository;
        this.typeRepository = typeRepository;
        this.redisService = redisService;
        this.sunatClient = sunatClient;
        this.util = util;
    }


    @Override
    public ResponseBase createEnterprise(RequestEnterprises requestEnterprises) {
        boolean validate = enterprisesValidations.validateInput(requestEnterprises);
        if(validate){
            EnterprisesEntity enterprises = getEntity(requestEnterprises);
            enterprisesTypeRespository.save(enterprises.getEnterprisesTypeEntity());
            typeRepository.save(enterprises.getDocumentsTypeEntity());
            enterprisesRepository.save(enterprises);
            // agregar a redis
            String redisData = util.convertToJsonEntity(enterprises);
            redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT+enterprises.getNumDocument(),redisData,10);
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprises));
        }else{
            return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT,Constants.MESS_ERROR_DATA_NOT_VALID,null);
        }
    }

    @Override
    public ResponseBase findOneEnterprise(String doc) {
        String redisCache = redisService.getValueByKey(Constants.REDIS_KEY_INFO_SUNAT+doc);
        if (redisCache!= null ) {
            EnterprisesEntity enterprisesEntity = util.convertFromJson(redisCache, EnterprisesEntity.class);
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));
        }
        EnterprisesEntity enterprisesEntity = enterprisesRepository.findByNumDocument(doc);
        redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT+enterprisesEntity.getNumDocument(),util.convertToJsonEntity(enterprisesEntity),10);
        return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));
    }

    @Override
    public ResponseBase findAllEnterprises() {
        // verificar si existe en redis
        String redisCache = redisService.getValueByKey(Constants.REDIS_KEY_INFO_SUNAT);
        if (redisCache!= null ) {
            EnterprisesEntity enterprisesEntity = util.convertFromJson(redisCache, EnterprisesEntity.class);
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));
        }
        Optional allEnterprises = Optional.of(enterprisesRepository.findAll());
        if(allEnterprises.isPresent()){
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS,allEnterprises);
        }
        return new ResponseBase(Constants.CODE_ERROR_DATA_NOT,Constants.MESS_ZERO_ROWS,Optional.empty());
    }

    @Override
    public ResponseBase updateEnterprise(Integer id, RequestEnterprises requestEnterprises) {
            Optional<EnterprisesEntity> enterprises = enterprisesRepository.findById(id);
            boolean validationEntity = enterprisesValidations.validateInputUpdate(requestEnterprises);
            if(validationEntity){
                EnterprisesEntity enterprisesUpdate = getEntityUpdate(requestEnterprises,enterprises.get());
                enterprisesRepository.save(enterprisesUpdate);
                return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS,Optional.of(enterprisesUpdate));
            }else {
                return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT,Constants.MESS_ERROR_DATA_NOT_VALID,Optional.empty());
            }

    }

    @Override
    public ResponseBase delete(Integer id) {

    return null;
    }
    public ResponseSunat getExecutionSunat (String numero){
        String redisCache = redisService.getValueByKey(Constants.REDIS_KEY_INFO_SUNAT+numero);
        if (redisCache!= null ) {
            ResponseSunat sunat = util.convertFromJson(redisCache, ResponseSunat.class);
            return sunat;
        }else {
            String authorizathion = "Bearer" + tokenSunat;
            ResponseSunat sunat = sunatClient.getInfoSunat(numero,authorizathion);
            //String redisData = util.convertToJsonEntity(sunat);
            //redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT+numero,redisData,Integer.valueOf(timeExperitionSunatInfo));
            return sunat;
        }
    }
    private EnterprisesEntity getEntity(RequestEnterprises requestEnterprises){
        EnterprisesEntity entity = new EnterprisesEntity();
        EnterprisesTypeEntity typeEntity = new EnterprisesTypeEntity();
        DocumentsTypeEntity documentsTypeEntity = new DocumentsTypeEntity();
        ResponseSunat sunat = getExecutionSunat(requestEnterprises.getNumDocument());
        if(sunat != null){
            entity.setBusinessName(sunat.getRazonSocial());
            entity.setNumDocument(sunat.getNumero());
            entity.setBusinessName(sunat.getRazonSocial());
            entity.setTradeName(sunat.getRazonSocial());
            entity.setStatus(Constants.STATUS_ACTIVE);
            // type_entity
            typeEntity.setCodType(Constants.COD_TYPE_RUC);
            typeEntity.setDescType(Constants.DESC_TYPE_SA);
            typeEntity.setStatus(Constants.STATUS_ACTIVE);
            typeEntity.setDateCreate(getTimestamp());
            typeEntity.setUserCreate(Constants.AUDIT_ADMIN);

            // documents_type_entity

            documentsTypeEntity.setCodType(Constants.COD_TYPE_RUC);
            documentsTypeEntity.setDescType(Constants.DESC_TYPE_SA);
            documentsTypeEntity.setStatus(Constants.STATUS_ACTIVE);

            entity.setEnterprisesTypeEntity(typeEntity);
            entity.setDocumentsTypeEntity(documentsTypeEntity);
        }else{
            return null;
        }
        return entity;
    }
    private EnterprisesEntity getEntityUpdate(RequestEnterprises requestEnterprises, EnterprisesEntity enterprisesEntity){
        enterprisesEntity.setNumDocument(requestEnterprises.getNumDocument());
        enterprisesEntity.setBusinessName(requestEnterprises.getBusinessName());
        enterprisesEntity.setEnterprisesTypeEntity(getEnterprisesType(requestEnterprises));
        enterprisesEntity.setUserModif(Constants.AUDIT_ADMIN);
        enterprisesEntity.setDateModif(getTimestamp());
        return enterprisesEntity;
    }
    private EnterprisesEntity getEntityDelete(EnterprisesEntity enterprisesEntity){

        return null;
    }

    private EnterprisesTypeEntity getEnterprisesType(RequestEnterprises requestEnterprises){
        EnterprisesTypeEntity typeEntity = new EnterprisesTypeEntity();

        typeEntity.setIdEnterprisesType(requestEnterprises.getEnterprisesTypeEntity());
        return typeEntity;
    }

    private DocumentsTypeEntity getDocumentsType(RequestEnterprises requestEnterprises){
        DocumentsTypeEntity typeEntity = typeRepository.findByCodType(Constants.COD_TYPE_RUC);
        return  typeEntity;
    }

    private Timestamp getTimestamp(){
        long currentTime = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(currentTime);
        return timestamp;
    }
}
