package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.PasswordResetPayload;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.*;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.entity.Role;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.RoleRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import com.keraune.vlvblueberrysystem.security.AvatarImageValidator;
import com.keraune.vlvblueberrysystem.security.CorporateEmailPolicy;
import com.keraune.vlvblueberrysystem.security.LoginIdentifier;
import com.keraune.vlvblueberrysystem.security.SecurityRoles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccountService {
    private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder; private final ApiRecordMapper mapper;
    private final CorporateEmailPolicy corporateEmailPolicy; private final AvatarImageValidator avatarValidator; private final AuditService audit;
    public AccountService(UserRepository users,RoleRepository roles,PasswordEncoder encoder,ApiRecordMapper mapper,CorporateEmailPolicy corporateEmailPolicy,AvatarImageValidator avatarValidator,AuditService audit){this.users=users;this.roles=roles;this.encoder=encoder;this.mapper=mapper;this.corporateEmailPolicy=corporateEmailPolicy;this.avatarValidator=avatarValidator;this.audit=audit;}
    @Transactional(readOnly=true) public User currentUser(){Authentication a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!a.isAuthenticated()||"anonymousUser".equals(a.getName()))throw new IllegalStateException("Sesión no autenticada");return users.findByUsernameIgnoreCase(a.getName()).orElseThrow(()->new IllegalStateException("Usuario autenticado no existe en MySQL"));}
    @Transactional(readOnly=true) public AuthenticatedUserResponse currentUserResponse(){return authenticatedUser(currentUser());}
    public AuthenticatedUserResponse authenticatedUser(User user){List<String> authorities=List.of("ROLE_"+(user.getRole()!=null?user.getRole().getNombre():SecurityRoles.CONSULTA));return response(user,authorities);}
    public AuthenticatedUserResponse authenticatedUser(Authentication auth){User user=users.findByUsernameIgnoreCase(auth.getName()).orElseThrow(()->new IllegalArgumentException("Usuario autenticado no encontrado"));return response(user,auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());}
    private AuthenticatedUserResponse response(User u,List<String> a){return new AuthenticatedUserResponse(u.getUsername(),u.getNombreCompleto(),u.getEmail(),u.getCargo(),u.getTelefono(),u.getAvatarColor(),u.getAvatarImage(),u.getRole()!=null?u.getRole().getNombre():null,Boolean.TRUE.equals(u.getRequiereCambioPassword()),a);}
    @Transactional(readOnly=true) public List<UserReferenceResponse> listUsers(){return users.findAllByOrderByNombreCompletoAsc().stream().map(mapper::user).toList();}
    @Transactional(readOnly=true) public List<String> activeRoles(){return roles.findByEstadoTrueOrderByNombreAsc().stream().map(Role::getNombre).toList();}
    public List<UserReferenceResponse> createUser(UserFormPayload payload){String username=LoginIdentifier.normalize(payload.username());String email=LoginIdentifier.normalize(payload.email());corporateEmailPolicy.validate(email);if(users.existsByUsernameIgnoreCase(username))throw new IllegalArgumentException("Ya existe un usuario con ese nombre de usuario.");if(users.existsByEmailIgnoreCase(email))throw new IllegalArgumentException("Ya existe un usuario con ese correo.");User u=new User();u.setUsername(username);u.setEmail(email);apply(u,payload,true);users.save(u);audit.record("USUARIOS","CREAR","User",u.getId(),u.getUsername(),"Se creó una cuenta corporativa.");return listUsers();}
    public List<UserReferenceResponse> updateUser(Long id,UserFormPayload payload){User u=user(id);String username=LoginIdentifier.normalize(payload.username());String email=LoginIdentifier.normalize(payload.email());corporateEmailPolicy.validate(email);users.findByUsernameIgnoreCase(username).filter(o->!o.getId().equals(id)).ifPresent(o->{throw new IllegalArgumentException("Ya existe otro usuario con ese nombre de usuario.");});users.findByEmailIgnoreCase(email).filter(o->!o.getId().equals(id)).ifPresent(o->{throw new IllegalArgumentException("Ya existe otro usuario con ese correo.");});assertAdminTransition(u,payload.rol(),payload.activo());boolean sensitive=!u.getUsername().equalsIgnoreCase(username)||!u.getRole().getNombre().equalsIgnoreCase(payload.rol())||Boolean.TRUE.equals(u.getEstado())!=payload.activo();u.setUsername(username);u.setEmail(email);apply(u,payload,false);if(sensitive)u.incrementSessionVersion();audit.record("USUARIOS","ACTUALIZAR","User",u.getId(),u.getUsername(),"Se actualizaron datos, rol o estado de una cuenta corporativa.");return listUsers();}
    public List<UserReferenceResponse> toggleUserStatus(Long id){User u=user(id);User current=currentUser();if(u.getId().equals(current.getId())&&Boolean.TRUE.equals(u.getEstado()))throw new IllegalArgumentException("No puedes desactivar tu propia cuenta mientras mantienes la sesión activa.");assertAdminTransition(u,u.getRole().getNombre(),!Boolean.TRUE.equals(u.getEstado()));u.setEstado(!Boolean.TRUE.equals(u.getEstado()));u.incrementSessionVersion();audit.record("USUARIOS",Boolean.TRUE.equals(u.getEstado())?"ACTIVAR":"DESACTIVAR","User",u.getId(),u.getUsername(),"Se cambió el estado de una cuenta corporativa.");return listUsers();}
    public void resetPassword(Long id,PasswordResetPayload payload){User u=user(id);u.setPassword(encoder.encode(payload.temporaryPassword()));u.setRequiereCambioPassword(true);u.incrementSessionVersion();audit.record("USUARIOS","RESTABLECER_CONTRASENA","User",u.getId(),u.getUsername(),"Se restableció la contraseña con cambio obligatorio en el siguiente acceso.");}
    public AuthenticatedUserResponse updateProfile(ProfileUpdatePayload p){User u=currentUser();String email=LoginIdentifier.normalize(p.email());corporateEmailPolicy.validate(email);users.findByEmailIgnoreCase(email).filter(o->!o.getId().equals(u.getId())).ifPresent(o->{throw new IllegalArgumentException("El correo ya está asociado a otra cuenta.");});u.setNombreCompleto(p.nombreCompleto().trim());u.setEmail(email);u.setCargo(trim(p.cargo()));u.setTelefono(trim(p.telefono()));u.setAvatarColor(defaultValue(p.avatarColor(),"emerald"));u.setAvatarImage(avatarValidator.validateAndNormalize(p.avatarImage()));audit.record("SESION","ACTUALIZAR_PERFIL","User",u.getId(),u.getUsername(),"El usuario actualizó su perfil.");return authenticatedUser(u);}
    public void changePassword(PasswordChangePayload p){User u=currentUser();if(!encoder.matches(p.currentPassword(),u.getPassword()))throw new IllegalArgumentException("La contraseña actual no coincide.");if(encoder.matches(p.newPassword(),u.getPassword()))throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual.");u.setPassword(encoder.encode(p.newPassword()));u.setRequiereCambioPassword(false);u.incrementSessionVersion();audit.record("SESION","CAMBIAR_CONTRASENA","User",u.getId(),u.getUsername(),"El usuario actualizó su contraseña.");}
    private void apply(User u,UserFormPayload p,boolean creating){Role role=roles.findByNombreIgnoreCase(p.rol()).filter(Role::getEstado).orElseThrow(()->new IllegalArgumentException("Rol no encontrado o inactivo: "+p.rol()));u.setRole(role);u.setNombreCompleto(p.nombreCompleto().trim());u.setCargo(trim(p.cargo()));u.setTelefono(trim(p.telefono()));u.setAvatarColor(defaultValue(p.avatarColor(),"emerald"));u.setEstado(p.activo());if(creating||(p.password()!=null&&!p.password().isBlank())){u.setPassword(encoder.encode(p.password()));u.setRequiereCambioPassword(creating);}}
    private void assertAdminTransition(User user,String requestedRole,boolean requestedActive){boolean wasAdmin=user.getRole()!=null&&SecurityRoles.ADMINISTRADOR.equalsIgnoreCase(user.getRole().getNombre())&&Boolean.TRUE.equals(user.getEstado());boolean remainsAdmin=SecurityRoles.ADMINISTRADOR.equalsIgnoreCase(requestedRole)&&requestedActive;if(wasAdmin&&!remainsAdmin&&users.countByRoleNombreIgnoreCaseAndEstadoTrue(SecurityRoles.ADMINISTRADOR)<=1)throw new IllegalArgumentException("Debe permanecer al menos una cuenta administrativa activa.");}
    private User user(Long id){return users.findById(id).orElseThrow(()->new IllegalArgumentException("Usuario no encontrado"));}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private String defaultValue(String v,String d){return v==null||v.isBlank()?d:v.trim();}
}
