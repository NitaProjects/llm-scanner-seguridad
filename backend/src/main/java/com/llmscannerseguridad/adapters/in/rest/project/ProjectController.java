package com.llmscannerseguridad.adapters.in.rest.project;

import java.net.URI;
import java.util.List;

import com.llmscannerseguridad.application.project.CreateProjectUseCase;
import com.llmscannerseguridad.application.project.DeleteProjectUseCase;
import com.llmscannerseguridad.application.project.GetProjectUseCase;
import com.llmscannerseguridad.application.project.ListProjectsUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping(value = "/api/projects", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectController {

    private final CreateProjectUseCase create;
    private final GetProjectUseCase get;
    private final ListProjectsUseCase list;
    private final DeleteProjectUseCase delete;
    private final ProjectRequestMapper requests;

    public ProjectController(CreateProjectUseCase create, GetProjectUseCase get,
            ListProjectsUseCase list, DeleteProjectUseCase delete, JsonMapper mapper) {
        this.create = create;
        this.get = get;
        this.list = list;
        this.delete = delete;
        requests = new ProjectRequestMapper(mapper);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectResponse> create(@RequestBody String body) {
        var project = create.execute(requests.creation(body).toCommand());
        return ResponseEntity.created(URI.create("/api/projects/" + project.id()))
                .body(ProjectResponse.from(project));
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable("id") String id) {
        return ProjectResponse.from(get.execute(requests.identifier(id)));
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return list.execute().stream().map(ProjectResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        delete.execute(requests.identifier(id));
        return ResponseEntity.noContent().build();
    }
}
