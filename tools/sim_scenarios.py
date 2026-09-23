import math
from sim_core import *
import sim_core as sim
# Hằng số cuối cùng, khớp với ReactorSim.java
sim.P.update(K_FUEL=1.30,ROD_NEIGH=4.0,XE_ABS=0.04,GEN=1.0,S0=1e-3,TIP_FUEL=2.0,TIP_LEN=0.35,GEN_PROMPT=0.05,BETA=0.06,WATER_ABS=0.05,VOID_SPAN=60.0)
def reg(s,set_pt,avg,dt,speed=0.12):
    if s.scram: s.rodTarget=1.0; s.speed=0.12; return
    s.speed=speed
    lp=math.log(max(avg,1e-6)); r=(lp-getattr(s,'lp',lp))/dt; s.lp=lp
    want=max(-0.05,min(0.03,math.log(max(set_pt,1e-3)/max(avg,1e-6))/30))
    err=r-want
    s.rodTarget=max(0.0,min(1.0,s.rod+err*2.0))
def go(s,secs,set_pt,dt=0.25,every=30,wf=1.0,label=""):
    F=types.count('F')
    avg=sum(s.phi[i] for i in range(len(cells)) if types[i]=='F')/F
    for k in range(int(secs/dt)):
        reg(s,set_pt,avg,dt)
        tot,maxT,a,st=step(s,dt,wf)
        avg=tot/(P['QPF']*H*F)
        if k%int(every/dt)==0: print(f"{label} t={k*dt:6.1f} rod={s.rod:.3f} avg={avg:7.3f} maxT={maxT:7.1f} void={a:.2f} Xmax={max(s.X):.3f}")
    return s
