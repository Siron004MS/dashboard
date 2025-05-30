package com.dashboard.myapp.web.rest;

import com.dashboard.myapp.domain.Job;
import com.dashboard.myapp.repository.JobRepository;
import com.dashboard.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.dashboard.myapp.domain.Job}.
 */
@RestController
@RequestMapping("/api/jobs")
@Transactional
public class JobResource {

    private static final Logger LOG = LoggerFactory.getLogger(JobResource.class);

    private static final String ENTITY_NAME = "job";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final JobRepository jobRepository;

    public JobResource(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * {@code POST  /jobs} : Create a new job.
     *
     * @param job the job to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new job, or with status {@code 400 (Bad Request)} if the job has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Job> createJob(@RequestBody Job job) throws URISyntaxException {
        LOG.debug("REST request to save Job : {}", job);
        if (job.getId() != null) {
            throw new BadRequestAlertException("A new job cannot already have an ID", ENTITY_NAME, "idexists");
        }
        job = jobRepository.save(job);
        return ResponseEntity.created(new URI("/api/jobs/" + job.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, job.getId().toString()))
            .body(job);
    }

    /**
     * {@code PUT  /jobs/:id} : Updates an existing job.
     *
     * @param id the id of the job to save.
     * @param job the job to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated job,
     * or with status {@code 400 (Bad Request)} if the job is not valid,
     * or with status {@code 500 (Internal Server Error)} if the job couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable(value = "id", required = false) final Long id, @RequestBody Job job)
        throws URISyntaxException {
        LOG.debug("REST request to update Job : {}, {}", id, job);
        if (job.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, job.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!jobRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        job = jobRepository.save(job);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, job.getId().toString()))
            .body(job);
    }

    /**
     * {@code PATCH  /jobs/:id} : Partial updates given fields of an existing job, field will ignore if it is null
     *
     * @param id the id of the job to save.
     * @param job the job to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated job,
     * or with status {@code 400 (Bad Request)} if the job is not valid,
     * or with status {@code 404 (Not Found)} if the job is not found,
     * or with status {@code 500 (Internal Server Error)} if the job couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Job> partialUpdateJob(@PathVariable(value = "id", required = false) final Long id, @RequestBody Job job)
        throws URISyntaxException {
        LOG.debug("REST request to partial update Job partially : {}, {}", id, job);
        if (job.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, job.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!jobRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Job> result = jobRepository
            .findById(job.getId())
            .map(existingJob -> {
                if (job.getJobTitle() != null) {
                    existingJob.setJobTitle(job.getJobTitle());
                }
                if (job.getMinSalary() != null) {
                    existingJob.setMinSalary(job.getMinSalary());
                }
                if (job.getMaxSalary() != null) {
                    existingJob.setMaxSalary(job.getMaxSalary());
                }

                return existingJob;
            })
            .map(jobRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, job.getId().toString())
        );
    }

    /**
     * {@code GET  /jobs} : get all the jobs.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of jobs in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Job>> getAllJobs(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "filter", required = false) String filter,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        if ("jobhistory-is-null".equals(filter)) {
            LOG.debug("REST request to get all Jobs where jobHistory is null");
            return new ResponseEntity<>(
                StreamSupport.stream(jobRepository.findAll().spliterator(), false).filter(job -> job.getJobHistory() == null).toList(),
                HttpStatus.OK
            );
        }
        LOG.debug("REST request to get a page of Jobs");
        Page<Job> page;
        if (eagerload) {
            page = jobRepository.findAllWithEagerRelationships(pageable);
        } else {
            page = jobRepository.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /jobs/:id} : get the "id" job.
     *
     * @param id the id of the job to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the job, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Job : {}", id);
        Optional<Job> job = jobRepository.findOneWithEagerRelationships(id);
        return ResponseUtil.wrapOrNotFound(job);
    }

    /**
     * {@code DELETE  /jobs/:id} : delete the "id" job.
     *
     * @param id the id of the job to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Job : {}", id);
        jobRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
