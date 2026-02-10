package io.bootify.my_tiendita.admin;

import io.bootify.my_tiendita.usuario.UsuarioDTO;
import io.bootify.my_tiendita.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(final UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    //  Página de login
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // Mostrar formulario
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("usuarioDTO", new UsuarioDTO());
        return "auth/register"; 
    }

    // Procesar formulario
    @PostMapping("/register")
public String register(@Valid @ModelAttribute UsuarioDTO usuarioDTO,
                      BindingResult bindingResult,
                      RedirectAttributes redirectAttributes,
                      Model model) { 
    System.out.println("Registrando nuevo usuario: " + usuarioDTO.getEmail());
    
    if (bindingResult.hasErrors()) {
        return "auth/register";  // Devuelve la vista con los errores
    }

    // Validar email duplicado
    if (usuarioService.emailExists(usuarioDTO.getEmail())) {
        bindingResult.rejectValue("email", "error.usuarioDTO", 
            "El email ya está registrado");
        return "auth/register";
    }

    try {
        usuarioService.registrarCliente(usuarioDTO);
        redirectAttributes.addFlashAttribute("success", 
            "¡Registro exitoso! Ya puedes iniciar sesión.");
        return "redirect:/login";
    } catch (Exception e) {
        model.addAttribute("error", "Error al registrar: " + e.getMessage());
        return "auth/register";
    }
  }
}