//#define SMALL_D 1e-10
// TODO: Consider making these structs of arrays instead of arrays of structs
// for better use of memory bandwidth across threads.
//
// These are primarily to conserve data transfer bandwidth, and memory usage.

typedef struct {
	float x;
	float y;
	float z;
} vec3; //12b

typedef struct {
	vec3 o; // origin
	vec3 d; // direction
	/* Pad this struct to a 32b boundary, may as well use the space? */
	float tnear; // smallest possible isect distance
	float tfar; // farthest possible isect distance
} ray; //32b

typedef struct {
	float t;
	float u;
	float v;
	int triangle;
} intersection; //16b


/*
 * Moller-Trumbore ray/triangle intersection (based on Java implementation).
 */
void ray_triangle_isect(
		const float4 origin, /* ray origin */
		const float4 direction, /* ray direction */
		const unsigned int triangleOffs, /* triangle index */
		float *t, /* max distance on input, isect dist on output if closer isect was found */
		float *u, /* output for barycentric isect coord */
		float *v, /* output for barycentric isect coord */
		unsigned int *hit_tri, /* output for triangle index if isect occurred */
		__global vec3 *tri_verts, /* vertex buffer */
		__global int *vert_indices) /* triplets of vertex indices for each triangle */
{
	/* Load triangle verts from global memory */
	__global vec3 *vert0 = &tri_verts[vert_indices[triangleOffs]];
	__global vec3 *vert1 = &tri_verts[vert_indices[triangleOffs + 1]];
	__global vec3 *vert2 = &tri_verts[vert_indices[triangleOffs + 2]];

	/* Compute edges */
	const float4 base_vert = (float4)(vert0->x, vert0->y, vert0->z, 0.0f);
	float4 e0 = (float4)(vert1->x, vert1->y, vert1->z, 0.0f) - base_vert;
	float4 e1 = (float4)(vert2->x, vert2->y, vert2->z, 0.0f) - base_vert;

	const float4 p = cross(direction, e1);
	const float divisor = dot(p, e0);
	/*
	 * Ray nearly parallel to triangle plane, or degenerate triangle...
	 */
	if (divisor < 1e-10 && divisor > -1e-10) {
		return;
	}

	const float4 translated_origin = origin - base_vert;
	const float4 q = cross(translated_origin, e0);
	/*
	 * Barycentric coords also result from this formulation, which could be useful for interpolating attributes
	 * defined at the vertex locations:
	 */
	const float e0dist = dot(p, translated_origin) / divisor;
	if (e0dist < 0 || e0dist > 1) {
		return;
	}

	const float e1dist = dot(q, direction) / divisor;
	if (e1dist < 0 || e1dist + e0dist> 1) {
		return;
	}

	const float isectDist = dot(q, e1) / divisor;

	if (isectDist > *t) {
		return;
	}

	/* Found intersection, store tri index, isect dist, and barycentric coords. */
	*t = isectDist;
	*u = e0dist;
	*v = e1dist;
	*hit_tri = triangleOffs / 3;
}

__kernel void find_intersections(
		__global intersection *hits,
		__global ray *rays,
		const unsigned int ray_count,
		__global vec3 *tri_verts,
		__global int *vert_indices,
		const unsigned int tri_count
		)
{
	const int ray_idx = get_global_id(0);
	/*
	 * Check to make sure we acutally have a ray to process in this thread.
	 * May enqueue more workitems than needed to fill workgroup.
	 */
	if (ray_idx >= ray_count) {
		return;
	}

	float4 ray_o;
	float4 ray_d;
	float maxT;
	/*
	 * As long as we access float4's at 16b aligned addresses
	 * this results in slightly more efficient access than pulling
	 * the ray components out piece by piece.
	 *
	 * TODO: This would permit memory coalescing if ray components were stored sequentially instead of interleaved.
	 */
	{
		__global float4 *ray_struct = (__global float4 *)&rays[ray_idx];
		float4 ray_struct0 = *ray_struct;
		++ray_struct;
		float4 ray_struct1 = *ray_struct;
		ray_o = (float4)(ray_struct0.xyz, 0.0f);
		ray_d = (float4)(ray_struct0.w, ray_struct1.xy, 0.0f);
		maxT = ray_struct1.w;
	}

	/*
	 * Iterate over all triangles to find closest intersection...
	 */
	unsigned int hit_tri = tri_count;
	float u, v;
	for (unsigned int tri_idx=0; tri_idx < tri_count; ++tri_idx) {
		ray_triangle_isect(	ray_o, ray_d, tri_idx * 3, &maxT,
				&u,	&v,	&hit_tri, tri_verts, vert_indices);
	}

	// Write intersection info to output buffer.


    hits[ray_idx].t = maxT;
	hits[ray_idx].u = u;
	hits[ray_idx].v = v;
	hits[ray_idx].triangle = hit_tri;
}

