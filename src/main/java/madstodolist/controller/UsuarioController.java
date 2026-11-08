package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.model.Usuario;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import madstodolist.dto.PerfilData;
import madstodolist.service.UsuarioServiceException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.validation.Valid;
import java.util.List;

@Controller
public class UsuarioController {
    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    @GetMapping("/registrados")
    public String registrados(Model model) {
        Long idUsuario = managerUserSession.usuarioLogeado();

        if (idUsuario == null || !usuarioService.esAdministrador(idUsuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permisos suficientes"
            );
        }

        List<UsuarioData> usuarios = usuarioService.todosUsuarios();
        UsuarioData usuarioLogueado = usuarioService.findById(idUsuario);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("usuarioLogueado", usuarioLogueado);
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcion(@PathVariable Long id, Model model) {
        Long idUsuario = managerUserSession.usuarioLogeado();

        if (idUsuario == null || !usuarioService.esAdministrador(idUsuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permisos suficientes"
            );
        }

        UsuarioData usuarioLogueado = usuarioService.findById(idUsuario);
        UsuarioData usuario = usuarioService.findById(id);

        //se hace esto para que si no hay usuario se redirige a la misma ya que no hay nada que mostrar
        if (usuario == null) {
            return "redirect:/registrados";
        }

        model.addAttribute("usuarioLogueado", usuarioLogueado);
        model.addAttribute("usuario", usuario);
        return "descripcionUsuario";
    }

    @PostMapping("/registrados/{id}/bloqueo")
    public String cambiarEstadoBloqueo(@PathVariable Long id) {

        Long idUsuario = managerUserSession.usuarioLogeado();

        if (idUsuario == null || !usuarioService.esAdministrador(idUsuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permisos suficientes"
            );
        }

        usuarioService.cambiarEstadoBloqueo(id);

        return "redirect:/registrados";
    }

    @GetMapping("/cuenta")
    public String cuenta(Model model){
        Long idUsuario = managerUserSession.usuarioLogeado();

        //si no esta logueado se va al login
        if (idUsuario == null) {
           return "redirect:/login";
        }

        UsuarioData usuario=usuarioService.findById(idUsuario);
        //si no se encuentra se va al login tambien
        if(usuario==null){
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarioLogueado", usuario);
        return "cuenta";
    }

    @GetMapping("/cuenta/editar")
    public String formEditarPerfil(Model model) {
        Long idUsuario = managerUserSession.usuarioLogeado();

        if (idUsuario == null) {
            return "redirect:/login";
        }

        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            return "redirect:/login";
        }

        //precargar los atributos que se pueden modificar
        PerfilData perfilData = new PerfilData();
        perfilData.setEmail(usuario.getEmail());
        perfilData.setNombre(usuario.getNombre());
        perfilData.setFechaNacimiento(usuario.getFechaNacimiento());

        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("perfilData", perfilData);
        return "formEditarPerfil";
    }

    @PostMapping("/cuenta/editar")
    public String editarPerfil(@Valid @ModelAttribute PerfilData perfilData, BindingResult result, Model model, RedirectAttributes flash) {
        Long idUsuario = managerUserSession.usuarioLogeado();

        if (idUsuario == null) {
            return "redirect:/login";
        }

        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) { //si la validacion de pefilData tuvo errores para no actualizar pero seguir mostranndo la plantilla de editar perfil
            model.addAttribute("usuarioLogueado", usuario);
            return "formEditarPerfil";
        }

        try {
            usuarioService.actualizarPerfil(idUsuario, perfilData.getEmail(), perfilData.getNombre(), perfilData.getFechaNacimiento());
        } catch (UsuarioServiceException e) {
            model.addAttribute("usuarioLogueado", usuario);
            model.addAttribute("error", e.getMessage());
            return "formEditarPerfil";
        }

        flash.addFlashAttribute("mensaje", "Perfil actualizado correctamente");
        return "redirect:/cuenta";
    }
}