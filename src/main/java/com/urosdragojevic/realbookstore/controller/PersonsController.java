package com.urosdragojevic.realbookstore.controller;

import com.urosdragojevic.realbookstore.audit.AuditLogger;
import com.urosdragojevic.realbookstore.domain.Person;
import com.urosdragojevic.realbookstore.domain.User;
import com.urosdragojevic.realbookstore.repository.PersonRepository;
import com.urosdragojevic.realbookstore.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.List;

@Controller
public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonsController.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/persons/{id}")
    @PreAuthorize("hasAuthority('VIEW_PERSON')")
    public String person(@PathVariable int id, Model model, HttpSession session) {

        auditLogger.audit("Korisnik je pristupio profilu osobe sa id=" + id);

        String csrf = session.getAttribute("CSRF_TOKEN").toString();
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
        model.addAttribute("person", personRepository.get("" + id));
        return "person";
    }

    @GetMapping("/myprofile")
    @PreAuthorize("hasAuthority('VIEW_MY_PROFILE')")
    public String self(Model model, Authentication authentication, HttpSession session) {
        User user = (User) authentication.getPrincipal();
        auditLogger.audit("Korisnik sa id=" + user.getId() + " je otvorio svoj profil");
        String csrf = session.getAttribute("CSRF_TOKEN").toString();
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
        model.addAttribute("person", personRepository.get("" + user.getId()));
        return "person";
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> person(@PathVariable int id) {
        personRepository.delete(id);
        userRepository.delete(id);

        auditLogger.audit("Obrisana je osoba i korisnik sa id=" + id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/update-person")
    public String updatePerson(Person person, HttpSession session, @RequestParam("csrfToken") String csrfToken,
                               HttpServletRequest request) throws AccessDeniedException {
        String csrf = session.getAttribute("CSRF_TOKEN").toString();
        if (!csrf.equals(csrfToken)) {
            auditLogger.audit("Neuspešan pokušaj ažuriranja osobe - CSRF token ne važi");
            throw new AccessDeniedException("Forbidden");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        boolean hasUpdateAuthority = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("UPDATE_PERSON"));
        boolean isUpdatingSelf = false;

        try {
            int personId = Integer.parseInt(person.getId());
            isUpdatingSelf = user.getId() == personId;
        } catch (NumberFormatException e) {
            auditLogger.audit("Neuspešan pokušaj ažuriranja – neispravan ID format: " + person.getId());
            throw new AccessDeniedException("Invalid person ID format");
        }

        if (!hasUpdateAuthority && !isUpdatingSelf) {
            auditLogger.audit("Korisnik sa id=" + user.getId() +
                    " je pokušao da ažurira osobu sa id=" + person.getId() + " bez dozvole");
            throw new AccessDeniedException("You are not authorized to update this person");
        }
        personRepository.update(person);
        auditLogger.audit("Korisnik sa id=" + user.getId() +
                " je uspešno ažurirao osobu sa id=" + person.getId());

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/myprofile")) {
            return "redirect:/myprofile";
        } else {
            return "redirect:/persons/" + person.getId();
        }
    }

    @GetMapping("/persons")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public String persons(Model model) {
        auditLogger.audit("Prikaz liste svih osoba");
        model.addAttribute("persons", personRepository.getAll());
        return "persons";
    }

    @GetMapping(value = "/persons/search", produces = "application/json")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    @ResponseBody
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        auditLogger.audit("Pretraga osobe: '" + searchTerm + "'");
        return personRepository.search(searchTerm);
    }
}
