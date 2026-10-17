package com.sangueplus.api.repositories

import com.sangueplus.api.models.PerfilDoacao
import org.springframework.data.jpa.repository.JpaRepository

interface PerfilDoacaoRepository :
    JpaRepository<PerfilDoacao, Long>