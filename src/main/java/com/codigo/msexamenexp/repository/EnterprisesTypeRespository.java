package com.codigo.msexamenexp.repository;

import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@Repository
public interface EnterprisesTypeRespository extends JpaRepository<EnterprisesTypeEntity, Integer>{
}
