package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Ingrediente;
import es.uca.orderflow.persistence.data.IngredienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GestionarIngredientes {
    private final IngredienteRepository ingredienteRepository;


    public  GestionarIngredientes(IngredienteRepository ingredienteRepository) {
        this.ingredienteRepository = ingredienteRepository;
    }


   public List<Ingrediente> obtenerIngredientes(){
        return ingredienteRepository.findAll();
    }

    public Ingrediente crearIngrediente(Ingrediente ingrediente){

        return ingredienteRepository.save(ingrediente);
    }

    public Ingrediente modificarIngrediente(Ingrediente ingrediente){
        if(!ingredienteRepository.existsById(ingrediente.getId())){
            throw new IllegalArgumentException("Ingrediente no encontrado");
        }
        return ingredienteRepository.save(ingrediente);
    }

    public void eliminarIngrediente(Ingrediente ingrediente){

        if(!ingredienteRepository.existsById(ingrediente.getId())){
            throw new IllegalArgumentException("Ingrediente no encontrado");
        }

        ingredienteRepository.deleteById(ingrediente.getId());
    }


    public Ingrediente obtenerIngredientePorId(Long id){
        return ingredienteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Ingrediente inexistente"));
    }

}
