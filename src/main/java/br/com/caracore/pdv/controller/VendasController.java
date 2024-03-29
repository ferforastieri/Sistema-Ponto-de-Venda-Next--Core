package br.com.caracore.pdv.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.caracore.pdv.model.ItemVenda;
import br.com.caracore.pdv.model.Operador;
import br.com.caracore.pdv.model.Produto;
import br.com.caracore.pdv.model.Venda;
import br.com.caracore.pdv.model.Vendedor;
import br.com.caracore.pdv.repository.filter.ProdutoFilter;
import br.com.caracore.pdv.repository.filter.VendaFilter;
import br.com.caracore.pdv.repository.filter.VendedorFilter;
import br.com.caracore.pdv.service.VendaService;
import br.com.caracore.pdv.service.exception.LojaNaoEncontradaException;
import br.com.caracore.pdv.service.exception.ProdutoNaoCadastradoException;
import br.com.caracore.pdv.service.exception.QuantidadeNaoExistenteEmEstoqueException;
import br.com.caracore.pdv.service.exception.VendedorNaoEncontradoException;
import br.com.caracore.pdv.util.Util;
import br.com.caracore.pdv.vo.CompraVO;

@Controller
@RequestMapping("/vendas")
public class VendasController {

	@Autowired
	private VendaService vendaService;

	@PostMapping("/produto")
	public ModelAndView pesquisarProduto(ProdutoFilter produtoFilter, BindingResult result, RedirectAttributes attributes) {

		Integer quantidade = null;
		Long codigoVenda = null;
		Long codigoVendedor = null;
		Long codigoProduto = null;
		String codigoBarra = null;
		
		Produto produto = null;
		Venda venda = null;
		Vendedor vendedor = null;
		
		
		String error = null;
		ModelAndView mv = new ModelAndView("venda/cadastro-venda");
		try {
			if (Util.validar(produtoFilter)) {
				if (Util.validar(produtoFilter.getCodigo())) {
					codigoProduto = Long.valueOf(produtoFilter.getCodigo());
				}
				if (Util.validar(produtoFilter.getQuantidade())) {
					quantidade = produtoFilter.getQuantidade();
				}
				if (Util.validar(produtoFilter.getCodigoBarra())) {
					codigoBarra = produtoFilter.getCodigoBarra();
				}
				if (Util.validar(produtoFilter.getCodigoVenda())) {
					codigoVenda = Long.valueOf(produtoFilter.getCodigoVenda());
				}
				if (Util.validar(produtoFilter.getCodigoVendedor())) {
					codigoVendedor = Long.valueOf(produtoFilter.getCodigoVendedor());
				}
			}
			
			// salvar vendedor na sessão
			if (Util.validar(codigoVendedor)) {
				vendedor = vendaService.buscarVendedorPorCodigo(codigoVendedor);
				if (Util.validar(vendedor)) {
					vendaService.salvarVendedorNaSessao(vendedor);
				}
			}
			
			if (Util.validar(codigoProduto)) {
				produto = vendaService.recuperarProdutoPorCodigo(codigoProduto);
			} else if (Util.validar(codigoBarra)) {
				produto = vendaService.recuperarProdutoPorCodigoBarra(codigoBarra);
			}
			
			venda = vendaService.comprar(produto, quantidade, codigoVenda, vendedor);
			
			// salvar venda na sessão
			if (Util.validar(venda)) {
				vendaService.salvarVendaNaSessao(venda);
			}
			
			return novo(venda);
			
		} catch (QuantidadeNaoExistenteEmEstoqueException ex) {
			error = ex.getMessage();
		} catch (ProdutoNaoCadastradoException ex) {
			error = ex.getMessage();
		} catch (LojaNaoEncontradaException ex) {
			error = ex.getMessage();
		} catch (VendedorNaoEncontradoException ex) {
			error = ex.getMessage();
		} catch (NumberFormatException ex) {
			error = ex.getMessage();
		}
		CompraVO compraVO = vendaService.recuperarSessao();
		if ((Util.validarOperador(compraVO)) && (Util.validarVenda(compraVO)) && (Util.validarVendedor(compraVO))) {
			Long codigoOperador = compraVO.getOperador().getCodigo();
			codigoVenda = compraVO.getVenda().getCodigo();
			codigoVendedor = compraVO.getVendedor().getCodigo();
			venda = buscarVendaEmAberto(codigoVenda, codigoVendedor);
			mv.addObject("vendedores", buscarVendedores(codigoOperador));
			mv.addObject(prepararFiltro(venda));
			mv.addObject(venda);
		} else {
			if ((Util.validarOperador(compraVO)) && (Util.validarVendedor(compraVO))) {
				Long codigoOperador = compraVO.getOperador().getCodigo();
				venda = Util.criarVendaVazia(compraVO.getVendedor());
				mv.addObject("vendedores", buscarVendedores(codigoOperador));
				mv.addObject(prepararFiltro(venda));
				mv.addObject(venda);
			}
		}
		mv.addObject("error", error);
		return mv;
	}
	
	@GetMapping("/novo")
	public ModelAndView novo(Venda venda) {
		CompraVO compraVO = vendaService.recuperarSessao();
		ModelAndView mv = new ModelAndView("venda/cadastro-venda");
		if ((Util.validar(venda)) && (Util.validar(venda.getCodigo()))) {
			if (Util.validar(venda.getVendedor())) {
				Long codigoVenda = venda.getCodigo();
				Long codigoVendedor = venda.getVendedor().getCodigo();
				venda = buscarVendaEmAberto(codigoVenda, codigoVendedor);
				if (Util.validarOperador(compraVO)) {
					Long codigoOperador = compraVO.getOperador().getCodigo();
					mv.addObject("vendedores", buscarVendedores(codigoOperador));
				}
			}
		} else {
			if ((Util.validarOperador(compraVO)) && (Util.validarVenda(compraVO)) && (Util.validarVendedor(compraVO))) {
				Long codigoOperador = compraVO.getOperador().getCodigo();
				Long codigoVenda = compraVO.getVenda().getCodigo();
				Long codigoVendedor = compraVO.getVendedor().getCodigo();
				venda = buscarVendaEmAberto(codigoVenda, codigoVendedor);
				mv.addObject("vendedores", buscarVendedores(codigoOperador));
			} else {
				if ((Util.validarOperador(compraVO)) && (Util.validarVendedor(compraVO))) {
					Long codigoOperador = compraVO.getOperador().getCodigo();
					venda = Util.criarVendaVazia(compraVO.getVendedor());
					mv.addObject("vendedores", buscarVendedores(codigoOperador));
					mv.addObject(prepararFiltro(venda));
					mv.addObject(venda);
				}
			}
		}
		mv.addObject(prepararFiltro(venda));
		mv.addObject(venda);
		return mv;
	}

	@GetMapping("/vendas")
	public ModelAndView pesquisarVendas(VendaFilter filtroVenda) {
		ModelAndView mv = new ModelAndView("venda/pesquisa-vendas");
		if (filtroVenda != null) {
			mv.addObject("vendas", vendaService.pesquisar(filtroVenda));
		} else {
			filtroVenda = new VendaFilter();
			filtroVenda.setVendedor("");
			filtroVenda.setLoja("");
		}
		mv.addObject(filtroVenda);
		return mv;
	}

	@GetMapping("/status/{codigo}")
	public ModelAndView cancelarVenda(@PathVariable Long codigo) {
		VendaFilter filtroVenda = new VendaFilter();
		filtroVenda.setVendedor("");
		filtroVenda.setLoja("");
		if (Util.validar(codigo)) {
			vendaService.cancelar(codigo);
			vendaService.apagarVendaDaSessão();
		}
		return pesquisarVendas(filtroVenda);
	}

	@GetMapping("{codigo}")
	public ModelAndView pesquisarVenda(@PathVariable Long codigo) {
		ModelAndView mv = new ModelAndView("venda/visualiza-venda");
		CompraVO sessao = vendaService.recuperarSessao();
		Long operadorId = sessao.getOperador().getCodigo();
		mv.addObject("vendedores", buscarVendedores(operadorId));
		Venda venda = vendaService.pesquisarPorId(codigo);
		mv.addObject(venda);
		return mv;
	}

	@PostMapping("/vendas")
	public ModelAndView pesquisarVendas(@Valid VendaFilter filtroVenda, BindingResult result,
			RedirectAttributes attributes) {
		ModelAndView mv = new ModelAndView("venda/pesquisa-vendas");
		if (filtroVenda != null) {
			mv.addObject("vendas", vendaService.pesquisar(filtroVenda));
		} else {
			filtroVenda = new VendaFilter();
			filtroVenda.setVendedor("");
		}
		return mv;
	}

	@GetMapping("/vendedores")
	public ModelAndView pesquisar(VendedorFilter filtroVendedor) {
		ModelAndView mv = new ModelAndView("venda/seleciona-vendedor");
		if (filtroVendedor != null) {
			mv.addObject("vendedores", vendaService.pesquisar(filtroVendedor));
		} else {
			filtroVendedor = new VendedorFilter();
			filtroVendedor.setNome("%");
		}
		return mv;
	}

	@GetMapping("/vendedor/{codigo}")
	public ModelAndView selecionar(@PathVariable Long codigo) {
		Venda venda = null;
		ModelAndView mv = new ModelAndView("venda/cadastro-venda");
		Vendedor vendedor = vendaService.recuperarVendedorPorId(codigo);
		if (Util.validar(vendedor)) {
			vendaService.salvarVendedorNaSessao(vendedor);
			venda = vendaService.recuperarVendaEmAberto(vendedor);
			if (!Util.validar(venda)) {
				venda = new Venda();
			}
			venda.setVendedor(vendedor);
			mv.addObject(vendedor);
		}
		return novo(venda);
	}

	@PostMapping("/novo")
	public ModelAndView salvar(@Valid Venda venda, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return novo(venda);
		}
		vendaService.salvar(venda);
		attributes.addFlashAttribute("mensagem", "Venda salva com sucesso!");
		return new ModelAndView("redirect:/vendas/novo");
	}
	
	/**
	 * Método interno para recuperar a venda
	 * 
	 * @param codigoVenda
	 * @param codigoVendedor
	 * @return
	 */
	private Venda buscarVendaEmAberto(Long codigoVenda, Long codigoVendedor) {
		Venda venda = null;
		if ((Util.validar(codigoVenda)) && (Util.validar(codigoVendedor))) {
			Vendedor vendedor = vendaService.recuperarVendedorPorId(codigoVendedor);
			if (Util.validar(vendedor)) {
				venda = vendaService.recuperarVendaEmAberto(codigoVenda);
				venda.setVendedor(vendedor);
			}
		}
		return venda;
	}

	/**
	 * Método para auxiliar no filtro de tela de pesquisa de vendas em aberto
	 * 
	 * @param venda
	 * @return
	 */
	private ProdutoFilter prepararFiltro(Venda venda) {
		Long codigoVenda = null;
		Long codigoVendedor = null;
		String strUltimoCodigo = "";
		Integer quantidade = vendaService.QUANTIDADE_UNITARIA;
		String strUltimoCodigoBarra = "";
		String strUltimaDescricao = "";
		if ((Util.validar(venda)) && (Util.validar(venda.getCodigo()))) {
			codigoVenda = venda.getCodigo();
		}
		if ((Util.validar(venda)) && (Util.validar(venda.getVendedor()))) {
			if (Util.validar(venda.getVendedor().getCodigo())) {
				codigoVendedor = venda.getVendedor().getCodigo();
			}
		}
		ItemVenda item = vendaService.recuperarUltimoItemVendaDaCesta(venda);
		if (Util.validar(item) && (Util.validar(item.getProduto()))) {
			strUltimoCodigo = item.getProduto().getCodigo().toString();
			if (Util.validar(item.getProduto().getCodigoBarra())) {
				strUltimoCodigoBarra = item.getProduto().getCodigoBarra().toString();
			}
			strUltimaDescricao = item.getProduto().getDescricao();
		}
		ProdutoFilter filtro = new ProdutoFilter(strUltimoCodigo, quantidade, strUltimoCodigoBarra, strUltimaDescricao, codigoVenda, codigoVendedor);
		return filtro;
	}

	/**
	 * Médoto para recuperar lista de vendedores da loja
	 * 
	 * @param operador
	 * @return
	 */
	private List<Vendedor> buscarVendedores(Long codigo) {
		List<Vendedor> vendedores = Util.criarListaDeVendedores();
		Operador operador = vendaService.recuperarOperadorPorId(codigo);
		if (Util.validar(operador)) {
			vendedores = vendaService.listarVendedoresPorOperador(operador);
			if (!Util.validar(vendedores)) {
				vendedores = Util.criarListaDeVendedores();
			}
		}
		return vendedores;
	}
}
