import {describe,it,expect,vi,beforeEach,afterEach} from 'vitest';
let request;
beforeEach(async()=>{vi.resetModules();vi.stubGlobal('fetch',vi.fn());({request}=await import('./api'));});
afterEach(()=>vi.unstubAllGlobals());
const response=(body,status=200)=>new Response(body===null?null:JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
describe('API transport',()=>{
 it('sends CSRF token on mutations and encodes JSON',async()=>{fetch.mockResolvedValueOnce(response({token:'csrf-token'})).mockResolvedValueOnce(response({id:1},201));await request('/register',{method:'POST',body:{name:'Jonas'}});expect(fetch).toHaveBeenLastCalledWith('/api/register',expect.objectContaining({body:'{"name":"Jonas"}',headers:{'Content-Type':'application/json','X-XSRF-TOKEN':'csrf-token'}}));});
 it('refreshes CSRF after login',async()=>{fetch.mockResolvedValueOnce(response({token:'before'})).mockResolvedValueOnce(response(null,204)).mockResolvedValueOnce(response({token:'after'})).mockResolvedValueOnce(response(null,204));await request('/login',{method:'POST',body:new URLSearchParams({username:'runner'})});await request('/logout',{method:'POST'});expect(fetch).toHaveBeenLastCalledWith('/api/logout',expect.objectContaining({headers:{'X-XSRF-TOKEN':'after'}}));});
 it('surfaces server validation messages',async()=>{fetch.mockResolvedValue(response({message:'Invalid date'},400));await expect(request('/admin/reports')).rejects.toThrow('Invalid date');});
 it('handles empty successful responses',async()=>{fetch.mockResolvedValue(response(null,204));await expect(request('/products')).resolves.toBeNull();});
});
