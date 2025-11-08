package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Empresa;
import es.uca.orderflow.persistence.data.EmpresaRepository;
import org.springframework.stereotype.Service;

@Service
public class ModificarEmpresa {

    private final EmpresaRepository empresaRepository;

    public ModificarEmpresa(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public Empresa modificarEmpresa(Empresa empresa) {
        return empresaRepository.save(empresa);
    }
}
