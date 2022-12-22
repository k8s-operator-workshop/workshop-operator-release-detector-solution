package com.workshop.operator.util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.workshop.operator.GitHubRelease;

@Path("/repos")
@RegisterRestClient
public interface GHService {

    @GET
    @Path("/{owner}/{repo}/releases/latest")
    GitHubRelease getByOrgaAndRepo(@PathParam(value = "owner") String owner, @PathParam(value = "repo") String repo);
}
