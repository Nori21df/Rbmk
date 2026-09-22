import math
R=5; H=6
cells=[(q,r) for q in range(-R,R+1) for r in range(-R,R+1) if max(abs(q),abs(r),abs(q+r))<=R]
idx={c:i for i,c in enumerate(cells)}
NB=[(1,0),(-1,0),(0,1),(0,-1),(1,-1),(-1,1)]
def ring(c): q,r=c; return max(abs(q),abs(r),abs(q+r))
types=[]
for (q,r) in cells:
    if (q,r)==(0,0): types.append('W')
    elif ring((q,r))>=R: types.append('G')
    elif (q-r)%4==0 and (q+2*r)%2==0: types.append('C')
    elif (q+r)%3==0: types.append('G')
    else: types.append('F')
print({t:types.count(t) for t in 'FGCW'})
nbrs=[[idx.get((c[0]+d[0],c[1]+d[1])) for d in NB] for c in cells]
P=dict(BETA=0.05,GEN_PROMPT=0.05,ROD_NEIGH=1.2,TIP_FUEL=0.5,K_FUEL=1.20,MOD=0.12,K_G=0.97,K_W=0.88,K_ROD_OUT=0.88,ROD_ABS=1.0,TIP_BONUS=0.35,TIP_LEN=0.25,
  WATER_ABS=0.10,VOID_SPAN=40.0,DOPPLER=0.00012,XE_ABS=0.15,GEN=0.12,S0=1e-4,W_SELF=2.0,
  T_SAT=280.0,H_WATER=10.0,H_PAS=0.2,HEAT_CAP=400.0,QPF=1000.0)
class S:
  def __init__(s):
    n=len(cells); s.phi=[0.0]*n; s.T=[20.0]*n; s.I=[0.0]*n; s.X=[0.0]*n; s.rod=1.0; s.rodTarget=1.0; s.speed=0.05; s.inserting=False
def step(s,dt,water_frac=1.0):
  p=P; n=len(cells)
  # rods
  if s.rod<s.rodTarget:
    s.inserting=True; s.rod=min(s.rodTarget,s.rod+s.speed*dt)
  elif s.rod>s.rodTarget:
    s.inserting=False; s.rod=max(s.rodTarget,s.rod-0.05*dt)
  else: s.inserting=False
  m=[0]*n; alpha=[0]*n
  for i,t in enumerate(types):
    a=1-math.exp(-max(0,s.T[i]-p['T_SAT'])/p['VOID_SPAN']); alpha[i]=a
    if t=='F':
      g=sum(1 for j in nbrs[i] if j is not None and types[j]=='G')
      c=sum(1 for j in nbrs[i] if j is not None and types[j]=='C')
      k=p['K_FUEL']*(1+p['MOD']*g/6)*(1-p['WATER_ABS']*(1-a))*(1-p['DOPPLER']*(s.T[i]-20))*(1-p['XE_ABS']*s.X[i])*max(0.0,1-p['ROD_NEIGH']*s.rod*c/6)
      m[i]=k
    elif t=='G': m[i]=p['K_G']
    elif t=='W': m[i]=p['K_W']
    else:
      k=p['K_ROD_OUT']*(1-s.rod*p['ROD_ABS'])
      if s.inserting and s.rod<p['TIP_LEN']: k+=p['TIP_BONUS']*(1-s.rod/p['TIP_LEN'])
      m[i]=k
  Pr=[s.phi[i]*m[i] for i in range(n)]
  F=[i for i in range(n) if types[i]=='F']
  aavg=sum(alpha[i] for i in F)/len(F)
  tip=1+p['TIP_FUEL']*(1-s.rod/p['TIP_LEN'])*(1-aavg) if (s.inserting and s.rod<p['TIP_LEN']) else 1.0
  new=[0]*n; tot=0; maxT=0; steam=0
  srcs=[tip*(p['W_SELF']*Pr[i]+sum(Pr[j] for j in nbrs[i] if j is not None))/(p['W_SELF']+6) for i in range(n)]
  sp=sum(s.phi); ge=(sum(srcs)/sp-1) if sp>1e-9 else 0
  g=p['GEN'] if ge<=p['BETA'] else p['GEN_PROMPT']
  for i in range(n):
    src=srcs[i]
    ph=src+(s.phi[i]-src)*math.exp(-dt/g)
    if types[i]=='F': ph+=p['S0']*dt
    new[i]=max(0,min(ph,1e4))
  s.phi=new
  for i in range(n):
    if types[i]!='F': continue
    Q=s.phi[i]*p['QPF']*H
    rem_w=water_frac*p['H_WATER']*H*max(0,s.T[i]-p['T_SAT'])
    rem=rem_w+p['H_PAS']*H*(s.T[i]-20)
    s.T[i]+=dt*(Q-rem)/(p['HEAT_CAP']*H)
    tot+=Q; steam+=rem_w; maxT=max(maxT,s.T[i])
    # xenon
    f=s.phi[i]
    s.I[i]=(s.I[i]+dt*f/60)/(1+dt/120)
    s.X[i]=(s.X[i]+dt*s.I[i]/120)/(1+dt*(1/180+f/60))
  return tot,maxT,sum(alpha[i] for i in range(n) if types[i]=='F')/types.count('F'),steam
def run(s,secs,dt=0.25,label="",every=20,wf=1.0):
  for k in range(int(secs/dt)):
    tot,maxT,a,st=step(s,dt,wf)
    if k%int(every/dt)==0: print(f"{label} t={k*dt:6.1f} rod={s.rod:.2f} phiavg={tot/(P['QPF']*H*types.count('F')):8.3f} maxT={maxT:7.1f} void={a:.2f} X={max(s.X):.3f}")
  return s
