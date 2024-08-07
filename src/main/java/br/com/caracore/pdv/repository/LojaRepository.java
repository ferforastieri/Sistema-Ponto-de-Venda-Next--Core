package br.com.caracore.pdv.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.caracore.pdv.model.Loja;

public interface LojaRepository extends JpaRepository<Loja, Long> {
	
	public List<Loja> findByNomeContainingIgnoreCase(String nome);
	
	public Loja findByNomeIgnoreCase(String nome);

}