delete from component_relation where (c_component_type ,c_component_version) in 
			(select c.c_component_type , c.c_component_version from component c where 
			c.i_component_id = 1 and c.i_status = 0)